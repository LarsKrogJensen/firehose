import java.util.*

fun main(args: Array<String>) {
    val map = TreeMap<Long, String>()
    map[2] = "2"
    map[12] = "12"
    map[22] = "22"
    map[15] = "15"
    map[18] = "18"
    map[54] = "54"
    map[29] = "29"
    map[45] = "45"

    val entry = map.ceilingEntry(20)
    println("ceiling: $entry")

    val floorKey = map.floorKey(1)
    println("floor: $floorKey")
//    map.tailMap(floorKey).forEach {
//        println(it.value)
//    }
//    println("Tail: ${map.tailMap(20).values}")

}