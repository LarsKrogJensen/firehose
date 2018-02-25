import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageConsumer

interface Behavoir {
    fun apply(command: Command)
    fun apply(event: TimeChanged)
}

class SocketGateVerticle(
    private val socketId: String,
    private val eventBusId: String
) : AbstractVerticle() {

    private val log = logger<SocketGateVerticle>()
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
            vertx.eventBus().send<List<TimeChanged>>("event.log", "1") { ar ->
                if (ar.succeeded()) {
                    log.info("successfully loaded log of ${ar.result().body().size} messages")
                    ar.result().body().forEach { event ->
                        vertx.eventBus().sendJsonFrame(socketId, Event(
                            eventType = EventType.TIME_OF_DAY,
                            timeOfDay = TimeChangedEvent(
                                hour = event.hour,
                                minute = event.minute,
                                second = event.second
                            ))
                        )
                    }
                } else {
                    log.error("failed to load log")
                }
            }
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