import io.vertx.core.Vertx
import io.vertx.kotlin.core.VertxOptions
import io.vertx.kotlin.core.net.NetServerOptions
import java.time.Instant
import java.time.OffsetDateTime

fun main(args: Array<String>) {
    val vertx = Vertx.vertx(VertxOptions(preferNativeTransport = true))
    println("Using native driver: " + vertx.isNativeTransportEnabled)

    val options = NetServerOptions(
        port = 1234
    )
    registerCodec<Init>(vertx.eventBus())
    registerCodec<TimeOfDay>(vertx.eventBus())
    registerCodec<OffsetDateTime>(vertx.eventBus())

    vertx.createNetServer(options)
        .connectHandler { socket ->
            vertx.deployVerticle(SocketVerticle(socketId = socket.writeHandlerID(), eventBusId = "eb-${socket.writeHandlerID()}")) { deployResult ->
                if (deployResult.succeeded()) {
                    val parser = frameParser(4) { buffer ->
                        vertx.eventBus().send("eb-${socket.writeHandlerID()}", buffer)
                    }
                    socket.handler {
                        println("Message received")
                        parser.handle(it)
                    }
                    socket.closeHandler {
                        println("Socket closed")
                        vertx.undeploy(deployResult.result())
                    }
                    socket.exceptionHandler {
                        println("socket exception ${it.message}")
                        vertx.undeploy(deployResult.result())
                    }
                } else {
                    println("Failed to deloy socket verticle ${deployResult.cause()}")
                    socket.close()
                }
            }
        }.listen {
            if (it.succeeded()) {
                println("Socket server listening")
                vertx.deployVerticle(TimeOfDayVerticle())
            }
            else
                println("Failed to listen ${it.cause()}")
        }
}

