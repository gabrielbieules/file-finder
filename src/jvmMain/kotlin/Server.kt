import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.encodeToString as myJsonEncode
import kotlinx.serialization.decodeFromString as myJsonDecode


val locations = HashSet<Location>()

val evidencesCache = ConcurrentHashMap<Int, HashSet<Evidence>>()

val serviceStatus = ServiceStatus(lastScanned = null, isScanning = false, filesNumber = 0)

private const val LOCAL_SNAPSHOT_FILE = "./localSnapshotFile.save"

fun main() {
    embeddedServer(Netty, 9090) {
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Delete)
            anyHost()
        }
        install(Compression) {
            gzip()
        }

        routing {
            route(ServiceStatus.path) {
                get {
                    call.respond(serviceStatus)
                }
            }

            route(Evidence.path) {
                get {
                    if (evidencesCache.isEmpty()) {
                        refreshEvidencesCache(log)
                    }
                    val search = call.parameters["search"] ?: ""
                    val minSize = call.parameters["minSize"]?.toLong()
                    val maxSize = call.parameters["maxSize"]?.toLong()

                    val pageIndex = call.parameters["pageIndex"]?.toInt() ?: 0
                    val pageSize = call.parameters["pageSize"]?.toInt() ?: 50

                    val words = search.split(" ")

                    val evidences = evidencesCache.values.flatten().filter { evidence ->
                        (words.isEmpty() || words.stream().allMatch {
                            evidence.filePath.contains(it, ignoreCase = true)
                                    || evidence.tags.contains(it) // FIXME: case sensitive?
                        })
                                && (minSize == null || evidence.fileSize >= minSize)
                                && (maxSize == null || evidence.fileSize <= maxSize)
                    }

                    val fromIndex = minOf(pageIndex * pageSize, evidences.size)
                    val toIndex = minOf((pageIndex + 1) * pageSize, evidences.size)
                    val subList = evidences.subList(fromIndex, toIndex)

                    call.respond(SearchResult(subList, evidences.size)) // TODO: need to return pagination info to UI
                }

                get("/{id}") {
                    val evidenceId = call.parameters["id"]?.toInt() ?: error("Invalid download request [ID]")
                    val evidence = evidencesCache.values.flatten().find { it.id == evidenceId }
                        ?: error("Invalid download request - $evidenceId not found")
                    val file = File(evidence.filePath)
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
                            evidence.fileName
                        )
                            .toString()
                    )
                    call.respondFile(file)
                }

                post("/{id}/tag/{tag}") {
                    val evidenceId = call.parameters["id"]?.toInt() ?: error("Invalid add tag request [ID]")
                    val evidence = evidencesCache.values.flatten().find { it.id == evidenceId }
                        ?: error("Invalid add tag request - $evidenceId not found")

                    val tag = call.parameters["tag"]

                    if (!tag.isNullOrBlank()) {
                        evidence.tags.add(tag)
                    }
                    call.respond(HttpStatusCode.OK)
                }

                delete("/{id}/tag/{tag}") {
                    val evidenceId = call.parameters["id"]?.toInt() ?: error("Invalid delete tag request [ID]")
                    val evidence = evidencesCache.values.flatten().find { it.id == evidenceId }
                        ?: error("Invalid delete tag request - $evidenceId not found")

                    val tag = call.parameters["tag"]

                    if (!tag.isNullOrBlank()) {
                        evidence.tags.remove(tag)
                    }
                    call.respond(HttpStatusCode.OK)
                }

                post {
                    launch {
                        refreshEvidencesCache(log)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            route(Location.path) {
                get {
                    call.respond(locations)
                }
                post {
                    val newLocation = call.receive<Location>()
                    if (locations.add(newLocation)) {
                        launch {
                            evidencesCache[newLocation.id] = scanLocation(newLocation, log)
                            serviceStatus.filesNumber = evidencesCache.values.flatten().size
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }
                delete("/{id}") {
                    val id = call.parameters["id"]?.toInt() ?: error("Invalid delete request")
                    if (locations.removeIf { location -> location.id == id }) {
                        evidencesCache.remove(id)
                        serviceStatus.filesNumber = evidencesCache.values.flatten().size
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("/snapshot/save") {
                log.info("Saving locations & evidences on local storage...")

                val json = Json.myJsonEncode(Cache(evidencesCache.toMap(), locations, serviceStatus))

                val file = File(LOCAL_SNAPSHOT_FILE)
                file.writeText(json)

                log.info("Successfully saved locations & evidences on local storage: {}", file.absolutePath)
                call.respond(HttpStatusCode.OK)
            }

            post("/snapshot/load") {
                val json = File(LOCAL_SNAPSHOT_FILE).readText(Charsets.UTF_8)

                val cache = Json.myJsonDecode<Cache>(json)

                locations.clear()
                locations.addAll(cache.locations)
                evidencesCache.clear()
                evidencesCache.putAll(cache.evidences)
                serviceStatus.lastScanned = cache.serviceStatus.lastScanned
                serviceStatus.filesNumber = evidencesCache.values.flatten().size

                log.info("Successfully loaded {} locations & {} evidences from local storage.",
                    locations.size, serviceStatus.filesNumber)

                call.respond(HttpStatusCode.OK)
            }

            // Server the frontend assets
            get("/") {
                call.respondText(
                    this::class.java.classLoader.getResource("index.html")!!.readText(),
                    ContentType.Text.Html
                )
            }
            static("/") {
                resources("")
            }
        }
    }.start(wait = true)
}

private fun refreshEvidencesCache(log: Logger) {
    if (serviceStatus.isScanning) {
        log.info("Scan already pending")
        return
    }
    serviceStatus.isScanning = true

    locations.map { location ->
        //FIXME: ConcurrentModificationException can occur
        evidencesCache[location.id] = scanLocation(location, log)
    }

    serviceStatus.isScanning = false
    serviceStatus.lastScanned = System.currentTimeMillis()
    serviceStatus.filesNumber = evidencesCache.values.flatten().size
}

private fun scanLocation(location: Location, log: Logger): HashSet<Evidence> {
    log.info("Start scanning root {}", location.filePath)

    val evidences = HashSet<Evidence>()

    val start = System.nanoTime()
    File(location.filePath).walk()
        .filter { !it.isDirectory }
        .forEach {
            evidences.add(Evidence(it.name, it.absolutePath, it.length()))
        }

    log.info(
        "Finished scanning {} files in {}ms from {}",
        evidences.size, (System.nanoTime() - start) / 1_000_000, location.filePath
    )

    return evidences
}