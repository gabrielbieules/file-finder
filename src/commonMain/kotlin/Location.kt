import kotlinx.serialization.Serializable

@Serializable
data class Location(val filePath: String) {
    val id: Int = filePath.hashCode()

    companion object {
        const val path = "/locations"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Location

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}