import io.vertx.core.Vertx
import io.vertx.core.net.OpenSSLEngineOptions
import io.vertx.kotlin.core.VertxOptions
import io.vertx.kotlin.core.net.NetClientOptions

fun main(args: Array<String>) {
    val vertx = Vertx.vertx(VertxOptions(preferNativeTransport = true))
    println("Using native driver: " + vertx.isNativeTransportEnabled)
    println("OpenSSL is available: " + OpenSSLEngineOptions.isAvailable())


    val netClientOptions = NetClientOptions(
        connectTimeout = 5_000,
        reconnectAttempts = 10,
        reconnectInterval = 500,
        ssl = true,
        trustAll = true,
        openSslEngineOptions = if (OpenSSLEngineOptions.isAvailable()) OpenSSLEngineOptions() else null

    )
    val client = vertx.createNetClient(netClientOptions)

    client.connect(1234, "localhost") { ar ->
        if (ar.succeeded()) {
            println("Connected")
            val socket = ar.result()
            socket.writeJsonFrame(SessionInitCommand(sequenceNo = 1))

            val parser = frameParser(4) {
                println("Message received: $it")
            }

            socket.handler(parser::handle)
            socket.exceptionHandler { ex ->
                println("Exception caught: " + ex)
            }
            socket.closeHandler {
                println("Socket closed")

            }
        } else {
            println("Failed to connect")
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {client.close()})
}