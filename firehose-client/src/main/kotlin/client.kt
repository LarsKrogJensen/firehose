import io.vertx.core.Vertx
import io.vertx.kotlin.core.VertxOptions
import io.vertx.kotlin.core.net.NetClientOptions

fun main(args: Array<String>) {
    val vertx = Vertx.vertx(VertxOptions(preferNativeTransport = true))
    println("Using native driver: " + vertx.isNativeTransportEnabled)

    val netClientOptions = NetClientOptions(
        connectTimeout = 5_000,
        reconnectAttempts = Int.MAX_VALUE,
        reconnectInterval = 5_000
    )
    val client = vertx.createNetClient(netClientOptions)

    client.connect(1234, "localhost") { ar ->
        if (ar.succeeded()) {
            println("Connected")
            val socket = ar.result()
            socket.writeJsonFrame(Init(sequenceNo = 1))

            val parser = frameParser(4) {
                println("Message received: $it")
            }

            with(socket) {
                handler(parser::handle)
                exceptionHandler { ex ->
                    println("Exception caught: " + ex)
                }
                closeHandler {
                    println("Socket closed")
                }
            }

        } else {
            println("Failed to connect")
        }
    }
}