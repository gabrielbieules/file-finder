import kotlinx.serialization.Serializable

@Serializable
data class Cache(val evidences: Map<Int, HashSet<Evidence>>, val locations: Set<Location>, val serviceStatus: ServiceStatus)