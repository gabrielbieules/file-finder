import kotlinx.serialization.Serializable

@Serializable
data class ServiceStatus(var lastScanned: Long?, var isScanning: Boolean, var filesNumber: Int) {

    companion object {
        const val path = "/serviceStatus"
    }
}