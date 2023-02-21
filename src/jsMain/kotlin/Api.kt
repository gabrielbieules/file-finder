import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.window

val endpoint = window.location.origin // only needed until https://youtrack.jetbrains.com/issue/KTOR-453 is resolved

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

suspend fun getServiceStatus(): ServiceStatus {
    return jsonClient.get(endpoint + ServiceStatus.path)
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
    return jsonClient.get(endpoint + Evidence.path + query)
}

suspend fun refreshEvidences() {
    jsonClient.post<Unit>(endpoint + Evidence.path)
}

suspend fun saveSnapshot() {
    jsonClient.post<Unit>("$endpoint/snapshot/save")
}
suspend fun loadSnapshot() {
    jsonClient.post<Unit>("$endpoint/snapshot/load")
}

suspend fun addTag(evidence: Evidence, tag: String) {
    jsonClient.post<Unit>(endpoint + Evidence.path + "/${evidence.id}/tag/$tag")
}

suspend fun deleteTag(evidence: Evidence, tag: String) {
    jsonClient.delete<Unit>(endpoint + Evidence.path + "/${evidence.id}/tag/$tag")
}

suspend fun getLocations(): List<Location> {
    return jsonClient.get(endpoint + Location.path)
}

suspend fun addLocation(location: Location) {
    jsonClient.post<Unit>(endpoint + Location.path) {
        contentType(ContentType.Application.Json)
        body = location
    }
}

suspend fun deleteLocation(location: Location) {
    jsonClient.delete<Unit>(endpoint + Location.path + "/${location.id}")
}
