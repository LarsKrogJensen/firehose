
import io.vertx.core.Vertx
import io.vertx.core.net.OpenSSLEngineOptions
import io.vertx.kotlin.core.VertxOptions

fun main(args: Array<String>) {
    val vertx = Vertx.vertx(VertxOptions(preferNativeTransport = true))
    println("Using native driver: " + vertx.isNativeTransportEnabled)
    println("OpenSSL is available: " + OpenSSLEngineOptions.isAvailable())

    vertx.deployVerticle(NetClientVerticle())
    Runtime.getRuntime().addShutdownHook(Thread {vertx.close()})
}