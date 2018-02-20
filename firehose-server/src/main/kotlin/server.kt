import io.vertx.core.Vertx
import io.vertx.kotlin.core.VertxOptions
import java.time.OffsetDateTime

fun main(args: Array<String>) {
    val vertx = Vertx.vertx(VertxOptions(preferNativeTransport = true))
    println("Using native driver: " + vertx.isNativeTransportEnabled)

    registerCodec<SessionInitCommand>(vertx.eventBus())
    registerCodec<TimeChangedEvent>(vertx.eventBus())
    registerCodec<OffsetDateTime>(vertx.eventBus())

    vertx.deployVerticle(NetServerVerticle()) { deployResult ->
        if (deployResult.succeeded()) {
            vertx.deployVerticle(TimeOfDayVerticle())
        } else {
            vertx.close()
        }
    }
}

