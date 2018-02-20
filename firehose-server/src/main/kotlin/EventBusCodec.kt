import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageCodec

class EventBusCodec<T>(private val type: Class<T>) : MessageCodec<T, T> {

    override fun encodeToWire(buffer: Buffer, obj: T) {}

    override fun decodeFromWire(pos: Int, buffer: Buffer): T? = null

    override fun transform(obj: T): T = obj

    override fun name(): String  = type.name

    override fun systemCodecID(): Byte = -1

}

inline fun <reified T: Any> registerCodec(eventBus: EventBus) {
    eventBus.registerDefaultCodec(T::class.java, EventBusCodec(T::class.java))
}