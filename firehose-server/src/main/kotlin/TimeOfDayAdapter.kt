import io.vertx.core.AbstractVerticle
import net.openhft.chronicle.bytes.BytesIn
import net.openhft.chronicle.bytes.BytesOut
import net.openhft.chronicle.bytes.ReadBytesMarshallable
import net.openhft.chronicle.bytes.WriteBytesMarshallable
import net.openhft.chronicle.wire.AbstractMarshallable
import net.openhft.chronicle.wire.Marshallable
import java.time.OffsetDateTime

data class TimeChanged(
    val hour: Int? = null,
    val minute: Int? = null,
    val second: Int? = null
) : AbstractMarshallable()

class TimeOfDayAdapter : AbstractVerticle() {
    private var timer: Long? = null
    private var lastTime = OffsetDateTime.now()

    override fun start() {
        timer = vertx.setPeriodic(1000) {
            val now = OffsetDateTime.now()
            vertx.eventBus().publish("time.of.day", TimeChanged(
                hour = if(now.hour != lastTime.hour) now.hour else null,
                minute = if(now.minute != lastTime.minute) now.minute else null,
                second = if(now.second != lastTime.second) now.second else null
            ))
            lastTime = now
        }
    }

    override fun stop() {
        timer?.let {
            vertx.cancelTimer(it)
        }
    }
}