package graveyard

import tools.TypeOfRegex

fun main() {
    val sampleSentence = "10년 3개월 6일 후 오후 4시"
    var result = TypeOfRegex.isNum.find(sampleSentence)
    while(result != null){
        println("v : ${result.value}, range : ${result.range}")
        result = result.next()
    }
    val sen2 = "DT_DURATION"
    result = Regex("^TI").find(sen2)

    println(result?.value)
}