import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(val evidences: List<Evidence>, val total: Int) {
    companion object {
        const val path = "/evidences"
    }
}