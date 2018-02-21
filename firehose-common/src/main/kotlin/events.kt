
enum class EventType {
    TIME_OF_DAY,
    HEARTBEAT
}

data class Event (
    val eventType: EventType,
    val timeOfDay: TimeChangedEvent?
)

data class TimeChangedEvent(
    val hour: Int?,
    val minute: Int?,
    val second: Int?
)