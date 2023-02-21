import kotlinx.serialization.Serializable

@Serializable
data class Evidence(val fileName: String, val filePath: String, val fileSize: Long) {
    val id: Int = filePath.hashCode()
    val tags: MutableSet<String> = HashSet()

    companion object {
        const val path = "/evidences"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Evidence

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}