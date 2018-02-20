import io.vertx.core.Vertx
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.core.VertxOptions
import io.vertx.kotlin.core.net.NetServerOptions
import java.nio.charset.Charset

fun main(args: Array<String>) {

    val vertx = Vertx.vertx(VertxOptions(preferNativeTransport = true))
    println("Using native driver: " + vertx.isNativeTransportEnabled)

    val options = NetServerOptions(
        port = 1234
    )
    
    vertx.createNetServer(options)
        .connectHandler { socket ->
            val parser = RecordParser.newFixed(4) { buffer ->
                println("socket ${socket.writeHandlerID()} got data " + buffer.toString(Charset.defaultCharset()))
            }
            socket.handler { buffer ->
                parser.handle(buffer)
            }
            socket.closeHandler {
                println("socket ${socket.writeHandlerID()} closed")
            }
            socket.exceptionHandler {
                println("socket exception ${it.message}")
            }
        }
        .listen {
            if (it.succeeded())
                println("Socket server listening")
            else
                println("Failed to listen ${it.cause()}")
        }
}