import io.vertx.core.buffer.Buffer
import io.vertx.core.parsetools.RecordParser

enum class FrameToken {
    PREFIX,
    PAYLOAD
}

fun frameParser(prefixSize: Int, handler: (buffer: Buffer) -> Any): RecordParser {
    var expectedToken = FrameToken.PREFIX

    val parser = RecordParser.newFixed(prefixSize)
    parser.handler { buffer ->
        when (expectedToken) {
            FrameToken.PREFIX -> {
                parser.fixedSizeMode(buffer.getInt(0))
                expectedToken = FrameToken.PAYLOAD
            }
            FrameToken.PAYLOAD -> {
                parser.fixedSizeMode(prefixSize)
                expectedToken = FrameToken.PREFIX
                handler(buffer)
            }
        }
    }

    return parser
}