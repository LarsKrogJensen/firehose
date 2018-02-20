import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageConsumer
import java.time.OffsetDateTime
import java.time.temporal.ChronoField

class SocketVerticle(
    private val socketId: String,
    private val eventBusId: String
) : AbstractVerticle() {
    private val evenBusConsumers = mutableListOf<MessageConsumer<*>>()

    override fun start(startFuture: Future<Void>) {
        val eventBus = vertx.eventBus()

        evenBusConsumers += eventBus.consumer<Buffer>(eventBusId) { message ->
            println("Socket verticle $socketId received message ${message.body()}")
        }
        evenBusConsumers += eventBus.consumer<OffsetDateTime>("time.of.day") { message ->
            // Convert and forward to eventBus which in turn will send on to socket
            eventBus.sendJsonFrame(socketId, TimeChangedEvent(
                hour = message.body().get(ChronoField.HOUR_OF_DAY),
                minute = message.body().get(ChronoField.MINUTE_OF_HOUR),
                second = message.body().get(ChronoField.SECOND_OF_MINUTE)
            ))
        }

        println("Socket verticle $socketId started")
        startFuture.complete()
    }

    override fun stop() {
        evenBusConsumers.forEach { it.unregister() }
        println("Socket verticle $socketId stopped")
    }
}