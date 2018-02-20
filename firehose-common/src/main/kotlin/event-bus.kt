import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus

fun EventBus.sendJsonFrame(address: String, obj: Any) {
    val msgBuf = Buffer.buffer().appendString(OBJECT_MAPPER.writeValueAsString(obj))
    this.send(address, Buffer.buffer().appendInt(msgBuf.length()).appendBuffer(msgBuf))
}