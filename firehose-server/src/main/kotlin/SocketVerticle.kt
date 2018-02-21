import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageConsumer
import java.time.OffsetDateTime

interface Behavoir {
    fun apply(command: Command)
    fun apply(event: TimeChanged)
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
            behavoir.apply(command.body().toObject<Command>())
        }
        evenBusConsumers += eventBus.consumer<TimeChanged>("time.of.day") { message ->
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
        override fun apply(event: TimeChanged) {}

        override fun apply(command: Command) {
            log.info("Socket verticle $socketId received command $command")
            behavoir = StreamingBehavior()
        }
    }

    inner class StreamingBehavior : Behavoir {
        override fun apply(command: Command) {}

        override fun apply(event: TimeChanged) {
            vertx.eventBus().sendJsonFrame(socketId, Event(
                eventType = EventType.TIME_OF_DAY,
                timeOfDay = TimeChangedEvent(
                    hour = event.hour,
                    minute = event.minute,
                    second = event.second
                ))
            )
        }
    }
}