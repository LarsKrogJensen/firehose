import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageConsumer
import java.time.OffsetDateTime

interface Behavoir {
    fun apply(command: Buffer): Behavoir
    fun apply(event: OffsetDateTime): Behavoir
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
            behavoir = behavoir.apply(command.body())
        }
        evenBusConsumers += eventBus.consumer<OffsetDateTime>("time.of.day") { message ->
            behavoir = behavoir.apply(message.body())
        }

        log.info("Socket verticle $socketId started")
        startFuture.complete()
    }

    override fun stop() {
        evenBusConsumers.forEach { it.unregister() }
        log.info("Socket verticle $socketId stopped")
    }


    inner class InitBehavior : Behavoir {
        override fun apply(command: Buffer): Behavoir {
            log.info("Socket verticle $socketId received command ${command}")
            return StreamingBehavior()
        }

        override fun apply(event: OffsetDateTime) = this
    }

    inner class StreamingBehavior : Behavoir {
        override fun apply(command: Buffer): Behavoir = this

        override fun apply(event: OffsetDateTime): Behavoir {
            // Convert and forward to eventBus which in turn will send on to socket
            vertx.eventBus().sendJsonFrame(socketId, TimeChangedEvent(
                hour = event.hour,
                minute = event.minute,
                second = event.second
            ))
            return this
        }
    }
}