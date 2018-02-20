import io.vertx.core.AbstractVerticle
import java.time.OffsetDateTime

class TimeOfDayVerticle : AbstractVerticle() {
    private var timer: Long? = null

    override fun start() {
        timer = vertx.setPeriodic(1000) {
            vertx.eventBus().publish("time.of.day", OffsetDateTime.now())
        }
    }

    override fun stop() {
        timer?.let {
            vertx.cancelTimer(it)
        }
    }
}