enum class CommandType {
    SESSION_INIT
}

data class Command (
    val commandType: CommandType,
    val sessionInit: SessionInitCommand?
)

data class SessionInitCommand(
    val sequenceNo: Int
)
