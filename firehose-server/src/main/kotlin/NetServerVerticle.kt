import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.net.NetSocket
import io.vertx.core.net.OpenSSLEngineOptions
import io.vertx.core.net.SelfSignedCertificate
import io.vertx.kotlin.core.net.NetServerOptions

class NetServerVerticle : AbstractVerticle() {
    val log = logger<NetServerVerticle>()

    override fun start(startFuture: Future<Void>) {
        var certificate = SelfSignedCertificate.create()

        OpenSSLEngineOptions.isAvailable()
        val options = NetServerOptions(
            port = 1234,
            ssl = true,
            pemKeyCertOptions = certificate.keyCertOptions(),
            pemTrustOptions = certificate.trustOptions(),
            openSslEngineOptions = if (OpenSSLEngineOptions.isAvailable()) OpenSSLEngineOptions() else null
        )
        vertx.createNetServer(options)
            .connectHandler(this::accept)
            .listen {
                if (it.succeeded()) {
                    log.info("Socket server listening")
                    startFuture.complete()
                } else {
                    log.info("Failed to listen ${it.cause()}")
                    startFuture.fail(it.cause())
                }
            }
    }

    private fun accept(socket: NetSocket) {
        val socketVerticle = SocketVerticle(socketId = socket.writeHandlerID(), eventBusId = "eb-${socket.writeHandlerID()}")
        vertx.deployVerticle(socketVerticle) { deployResult ->
            if (deployResult.succeeded()) {
                val parser = frameParser(4) { buffer ->
                    vertx.eventBus().send("eb-${socket.writeHandlerID()}", buffer)
                }
                socket.handler {
                    parser.handle(it)
                }
                socket.closeHandler {
                    log.info("socket closed")
                    vertx.undeploy(deployResult.result())
                }
                socket.exceptionHandler {
                    log.error("socket exception ${it.message}")
                    vertx.undeploy(deployResult.result())
                }
            } else {
                log.error("Failed to deloy socket verticle ${deployResult.cause()}")
                socket.close()
            }
        }
    }
}