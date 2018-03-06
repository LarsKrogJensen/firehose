
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import java.util.concurrent.CountDownLatch

data class Msg(
    val id: Int,
    val start: Long
)

data class Metric(
    val max: Long,
    val min: Long,
    val total: Long,
    val count: Int
)

class Stage(val input: String, val output: String) : AbstractVerticle() {

    override fun start() {
        vertx.eventBus().consumer<Msg>(input) {
            vertx.eventBus().send(output, it.body())
        }
        println("Started Stage-($input->$output)")
    }
}


fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val first = 0
    val last = 10

    registerCodec<Msg>(vertx.eventBus())
    val startLatch = CountDownLatch(last)
    for(i in first until last) {
        vertx.deployVerticle(Stage(i.toString(), (i+1).toString())) {
          startLatch.countDown()
        }
    }

    startLatch.await()
    println("All started")

    runTest(vertx, 10_000, first, last)
    val (max, min, total, count) = runTest(vertx, 1_000_000, first, last)

    println("Max: $max Min: $min Total: $total Avg: ${total/count}")

    vertx.close()
}

fun runTest(vertx: Vertx, count: Int, pipelineIn: Int, pipelineOut: Int): Metric {
    var max = Long.MIN_VALUE
    var min = Long.MAX_VALUE
    var total = 0L

    val msgLatch = CountDownLatch(count)
    val eventBus = vertx.eventBus()
    val consumer = eventBus.consumer<Msg>(pipelineOut.toString()) { m ->
        val duration = System.currentTimeMillis() - m.body().start
        total += duration
        max = Math.max(max, duration)
        min = Math.min(min, duration)
        msgLatch.countDown()
    }

    for (i in 0 until count) {
        eventBus.send(pipelineIn.toString(), Msg(i, System.currentTimeMillis()))
    }

    msgLatch.await()
    println("Completed")
    consumer.unregister()

    return Metric(
        max,
        min,
        total,
        count
    )
}

