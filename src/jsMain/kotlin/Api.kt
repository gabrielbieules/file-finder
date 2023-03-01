import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


val jsonClient = HttpClient(Js) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
}

suspend fun getServiceStatus(): ServiceStatus {
    return jsonClient.get(ServiceStatus.path).body()
}

suspend fun searchEvidences(search: String, minSize: String?, maxSize: String?, pageIndex: Int?, pageSize: Int?): SearchResult {
    var query = "?search=$search"
    if (!minSize.isNullOrBlank()) {
        query += "&minSize=$minSize"
    }
    if (!maxSize.isNullOrBlank()) {
        query += "&maxSize=$maxSize"
    }
    if (pageIndex != null) {
        query += "&pageIndex=$pageIndex"
    }
    if (pageSize != null) {
        query += "&pageSize=$pageSize"
    }
    return jsonClient.get(Evidence.path + query).body()
}

suspend fun refreshEvidences() {
    jsonClient.post(Evidence.path)
}

suspend fun saveSnapshot() {
    jsonClient.post("/snapshot/save")
}
suspend fun loadSnapshot() {
    jsonClient.post("/snapshot/load")
}

suspend fun addTag(evidence: Evidence, tag: String) {
    jsonClient.post(Evidence.path + "/${evidence.id}/tag/$tag")
}

suspend fun deleteTag(evidence: Evidence, tag: String) {
    jsonClient.delete(Evidence.path + "/${evidence.id}/tag/$tag")
}

suspend fun getLocations(): List<Location> {
    return jsonClient.get(Location.path).body()
}

suspend fun addLocation(location: Location) {
    jsonClient.post(Location.path) {
        contentType(ContentType.Application.Json)
        setBody(location)
    }
}

suspend fun deleteLocation(location: Location) {
    jsonClient.delete(Location.path + "/${location.id}")
}
