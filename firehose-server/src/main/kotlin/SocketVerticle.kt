import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageConsumer
import java.time.OffsetDateTime

interface Behavoir {
    fun apply(command: Buffer)
    fun apply(event: OffsetDateTime)
}

class SocketVerticle(
    private val socketId: String,
    private val eventBusId: String
) : AbstractVerticle() {

    private val log = logger<SocketVerticle>()
    private val evenBusConsumers = mutableListOf<MessageConsumer<*>>()
    private var behavoir: Behavoir = InitBehavior()

    override fun start(startFuture: Future<Void>) {
        val eventBus = vertx.eventBus()

        evenBusConsumers += eventBus.consumer<Buffer>(eventBusId) { command ->
            behavoir.apply(command.body())
        }
        evenBusConsumers += eventBus.consumer<OffsetDateTime>("time.of.day") { message ->
            behavoir.apply(message.body())
        }

        log.info("Socket verticle $socketId started")
        startFuture.complete()
    }

    override fun stop() {
        evenBusConsumers.forEach { it.unregister() }
        log.info("Socket verticle $socketId stopped")
    }


    inner class InitBehavior : Behavoir {
        override fun apply(event: OffsetDateTime) {}

        override fun apply(command: Buffer) {
            log.info("Socket verticle $socketId received command ${command}")
            behavoir = StreamingBehavior()
        }
    }

    inner class StreamingBehavior : Behavoir {
        override fun apply(command: Buffer) {}

        override fun apply(event: OffsetDateTime) {
            vertx.eventBus().sendJsonFrame(socketId, TimeChangedEvent(
                hour = event.hour,
                minute = event.minute,
                second = event.second
            ))
        }
    }
}