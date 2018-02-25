
import com.fasterxml.jackson.module.kotlin.readValue
import io.vertx.core.AbstractVerticle
import net.openhft.chronicle.queue.RollCycles
import net.openhft.chronicle.queue.impl.StoreFileListener
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import java.io.File
import java.nio.file.Paths
import java.util.*


class EventLogVericle : AbstractVerticle() {
    private val log = logger<EventLogVericle>()
    private lateinit var queue: SingleChronicleQueue
    private val indexLookup: TreeMap<Long, Pair<Int,Int>> = TreeMap()

    override fun start() {
        val queueDir = "C:\\Users\\larsk\\AppData\\Local\\Temp\\chronicle-queue14443099216027694310\\"
//        val queueDir = Files.createTempDirectory("chronicle-queue").toFile()
        var currentCycle: Int
        var currentIndex = 0

        queue = SingleChronicleQueueBuilder.binary(queueDir)
            .rollCycle(RollCycles.MINUTELY)
            .storeFileListener { cycle, file ->
                currentCycle = cycle
                currentIndex = 0
                log.info("File released ${file!!.absolutePath} cycle $cycle")
                try {
                    file.delete()
                } catch (e: Exception) {
                    println(e)
                }
            }
            .build()


        currentCycle = RollCycles.MINUTELY.toCycle(0)

        val appender = queue.acquireAppender()
        val tailer = queue.createTailer()
        vertx.eventBus().consumer<TimeChanged>("time.of.day") { message ->
            appender.writeText(OBJECT_MAPPER.writeValueAsString(message.body()))
            indexLookup += System.nanoTime() to Pair(currentCycle, currentIndex)
            currentIndex++
        }

        vertx.eventBus().consumer<String>("event.log") { message ->
            tailer.moveToIndex(0)
            val log = mutableListOf<TimeChanged>()

            val start = System.currentTimeMillis()
            var jsonText = tailer.readText()
            while (jsonText != null) {
                log += OBJECT_MAPPER.readValue<TimeChanged>(jsonText)
                jsonText = tailer.readText();
            }
            val end = System.currentTimeMillis()
            println("Loaded ${log.size} in ${end-start} ms")

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