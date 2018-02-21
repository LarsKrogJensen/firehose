import io.vertx.core.Vertx
import io.vertx.core.net.OpenSSLEngineOptions
import io.vertx.kotlin.core.VertxOptions
import java.time.OffsetDateTime

fun main(args: Array<String>) {
    val vertx = Vertx.vertx(VertxOptions(preferNativeTransport = true))
    println("Using native driver: " + vertx.isNativeTransportEnabled)
    println("OpenSSL is available: " + OpenSSLEngineOptions.isAvailable())

    registerCodec<Command>(vertx.eventBus())
    registerCodec<Event>(vertx.eventBus())
    registerCodec<TimeChanged>(vertx.eventBus())
    registerCodec<OffsetDateTime>(vertx.eventBus())

    vertx.deployVerticle(NetServerVerticle()) { deployResult ->
        if (deployResult.succeeded()) {
            vertx.deployVerticle(TimeOfDayAdapter())
        } else {
            println("Failed to deploy ${deployResult.cause()}")
            vertx.close()
        }
    }
}

