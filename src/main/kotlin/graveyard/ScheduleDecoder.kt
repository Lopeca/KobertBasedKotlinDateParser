import scheduleItem.ItemDate
import java.util.regex.Pattern

fun main(){
    println("Another Main")

    val sentence: MutableList<TaggedWord> = mutableListOf()
    val sampleSentence = "2015.9.8.(화)+e"

    val ymd = Regex("\\D*(\\d*[\\./년\\s])?\\s?(\\d*[\\./월\\s])?\\s?(\\d+)[^-~]?")
    val md = Regex("\\D*(\\d+)[\\./월\\s]\\s?(\\d+)[^-~]?")


    val sen2 = "6월2일   2015년 6월3일"
    val sen3 = "6월 2일"
    val sen4 = "2019.10.1.()"
    val sen5 = " 2"
    tagToSentenceAI(sampleSentence)  //여기서 sentence 완성됨. 원래 활용은 sentence = tagToSentence(sampleSentence)가 된다

    // sentence가 임의로 완성됐다고 가정
    val word = TaggedWord("2015.9.8.(화)+e", Tags.DT_OTHERS)
    sentence.add(word)

    var resYmd = ymd.find(sen2)
    printDestructured(resYmd)
    resYmd = resYmd?.next()
    printDestructured(resYmd)
    resYmd = ymd.find(sen3)
    printDestructured(resYmd)
    resYmd = ymd.find(sen4)
    printDestructured(resYmd)
    resYmd = ymd.find(sen5)
    printDestructured(resYmd)
    resYmd = ymd.find(sampleSentence)
    printDestructured(resYmd)

    var resMd = md.find(sen5)

    val dateList:MutableList<ItemDate> = mutableListOf()

    while(resYmd != null){
        val(year, month, day) = resYmd.destructured
        //val(month2, day2) = resMd!!.destructured

        printDestructured(resYmd)
        println("year : $year || month : $month || day : $day")
        println("")
        println("")

//        println("month2 : $month2")
//        println("day2 : $day2")

        resYmd = resYmd.next()
//        resMd = resMd.next()
       // dateList.add(ItemDate(year.toInt(),month.toInt(),day.toInt()))
    }
    var range:IntRange? = 1..15
    print("${range!!.last}")
    print("${range.first}")
    print("${range!!.step}")

}

fun printDestructured(res:MatchResult?){
    if(res!=null)
        println(res.destructured.toList())
}
