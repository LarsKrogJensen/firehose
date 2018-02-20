
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.core.VertxOptions
import io.vertx.kotlin.core.net.NetClientOptions

fun main(args: Array<String>) {
    val vertx = Vertx.vertx(VertxOptions(preferNativeTransport = true))

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

            val msgBuf = Buffer.buffer().appendString("{Hello}")
            socket.write(Buffer.buffer().appendInt(msgBuf.length()).appendBuffer(msgBuf))

            val parser = RecordParser.newFixed(4) {
                println("Message received: $it")
            }

            socket.closeHandler {
                println("Socket closed")
            }.handler {
                    parser.handle(it)
                }.exceptionHandler {
                    println("Exception caught: " + it)
                }

        } else {
            println("Failed to connect")
        }
    }
}