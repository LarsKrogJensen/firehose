import io.vertx.core.AbstractVerticle
import net.openhft.chronicle.queue.ChronicleQueue
import net.openhft.chronicle.queue.ExcerptAppender
import net.openhft.chronicle.queue.RollCycles
import net.openhft.chronicle.queue.impl.StoreFileListener
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import java.io.File
import java.nio.file.Paths


interface MessageListener {
    fun timeChanged(msg: TimeChanged)
}

class EventLogVericle : AbstractVerticle() {
    private lateinit var appender: ExcerptAppender
    private lateinit var queue: ChronicleQueue
    override fun start() {
        val queueDir = Paths.get("/tmp/chronicle")
        queue = SingleChronicleQueueBuilder.binary(queueDir)
            .rollCycle(RollCycles.MINUTELY)
            .storeFileListener { cycle, file ->
                println("File released ${file!!.absolutePath} cycle $cycle")
                file.delete()
            }
            .build()

        val writer = queue.acquireAppender().methodWriter(MessageListener::class.java)

        vertx.eventBus().consumer<TimeChanged>("time.of.day") { message ->
            writer.timeChanged(message.body())
        }

        vertx.eventBus().consumer<String>("event.log") { message ->
            val tailer = queue.createTailer()
            tailer.moveToIndex(0)
            val log = mutableListOf<TimeChanged>()
            val reader = tailer.methodReader(object: MessageListener{
                override fun timeChanged(msg: TimeChanged) {
                    println(msg)
                    log += msg
                }
            })
            while(reader.readOne());

            message.reply(log)
        }
    }
}

class RollingListener : StoreFileListener {
    override fun onReleased(cycle: Int, file: File?) {
        println("File released ${file!!.absolutePath} cycle $cycle")
    }

}

fun main(args: Array<String>) {
    val queueDir = Paths.get("/tmp/chronicle")
//    val queueDir = Files.createTempDirectory("chronicle-queue").toFile()
    val chronicle = SingleChronicleQueueBuilder.binary(queueDir)
        .rollCycle(RollCycles.TEST_SECONDLY)
        .storeFileListener(RollingListener())
        .build()

    val appender = chronicle.acquireAppender()

    appender.writeText("Hej")
    appender.writeText("Hopp")
    appender.writeText("Galopp")

    val tailer = chronicle.createTailer()
    tailer.moveToIndex(0)


    var readText: String? = tailer.readText()
    while (readText != null) {
        println(readText)
        readText = tailer.readText()
    }
}