
import io.vertx.core.AbstractVerticle
import io.vertx.core.net.NetClient
import io.vertx.core.net.OpenSSLEngineOptions
import io.vertx.kotlin.core.net.NetClientOptions
import java.util.concurrent.TimeUnit.SECONDS

class NetClientVerticle : AbstractVerticle() {
    val log = logger<NetClientVerticle>()
    var client: NetClient? = null
    var timer: Long? = null

    override fun start() {
        val netClientOptions = NetClientOptions(
            connectTimeout = 5_000,
            reconnectAttempts = 10,
            reconnectInterval = 500,
            ssl = true,
            trustAll = true,
            openSslEngineOptions = if (OpenSSLEngineOptions.isAvailable()) OpenSSLEngineOptions() else null
        )
        client = vertx.createNetClient(netClientOptions)
        connect()
    }

    override fun stop() {
        log.info("Stopping client")
        client?.close()
        timer?.let { vertx.cancelTimer(it) }
    }

    private fun connect() {
        log.info("Connecting...")
        client!!.connect(1234, "localhost") { ar ->
            if (ar.succeeded()) {
                log.info("Connected")
                val socket = ar.result()
                vertx.setTimer(SECONDS.toMillis(2)) {
                    socket.writeJsonFrame(SessionInitCommand(sequenceNo = 1))
                }

                val parser = frameParser(4) {
                    log.info("Message received: $it")
                }

                socket.handler(parser::handle)
                socket.exceptionHandler { ex ->
                    log.error("Exception caught: ${ex.message}")
                    reconnect()
                }
                socket.closeHandler {
                    log.info("Socket closed")
                }
            } else {
                log.error("Failed to connect")
                reconnect()
            }
        }
    }

    private fun reconnect() {
        timer = vertx.setTimer(SECONDS.toMillis(5)) {
            log.info("Reconnecting...")
            connect()
        }
    }
}