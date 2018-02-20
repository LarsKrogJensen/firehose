data class SessionInitCommand(
    val sequenceNo: Int
)

data class TimeChangedEvent(
    val hour: Int,
    val minute: Int,
    val second: Int
)