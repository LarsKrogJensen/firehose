import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket

fun NetSocket.writeJsonFrame(obj: Any) {
    val msgBuf = Buffer.buffer().appendString(OBJECT_MAPPER.writeValueAsString(obj))
    this.write(Buffer.buffer().appendInt(msgBuf.length()).appendBuffer(msgBuf))
}