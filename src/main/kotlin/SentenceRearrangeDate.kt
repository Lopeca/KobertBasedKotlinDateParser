import scheduleItem.ItemDate

fun main(){
    // 정규표현식
    val ymd = Regex("\\D*(\\d*[\\./년\\s])?\\s?(\\d*[\\./월\\s])?\\s?(\\d+)[^-~]?")
    val extNum = Regex("\\D")

    // 지금 커밋한 데이터는 ner_train_data(17)의 문장입니다
    // 아래 taggedWords가 완성된 시점까지는 학습해서 데이터 받아올 때 필요 없을 수 있는 코드
    val originalSentence = "건축가 <건축가 명사특강> 2019.8.27 -9.24 어디서 살 것인가 9.3 180* 매주 (화) 16:00 - 18:00 세종시청 4층 여민실 정태인 |"
    val originalTag = "O O O DT_OTHERS DT_OTHERS O O O DT_OTHERS O DT_OTHERS DT_OTHERS TL_OTHERS O TL_OTHERS O O O O O"

    val beforeWords = originalSentence.split(" ")
    val beforeTags = originalTag.split(" ")

    println(beforeWords.size)
    println(beforeTags.size)

    val taggedWords:MutableList<TaggedWord> = mutableListOf()

    for(i in beforeWords.indices){
        val currentTag:Int = convertTagStringToInt(beforeTags[i])
        taggedWords.add(TaggedWord(beforeWords[i], currentTag))
    }
    println(taggedWords)
    // 여기까지 인공지능 받을 때 달라질 코드


    // 1. DT태그가 연속해서 출현하면 붙여서 저장
    val dtCollector:MutableList<StringBuilder> = mutableListOf()
    var temp = StringBuilder()
    for (item in taggedWords) {
        val currentTag = item.tag
        if (!isTagDT(currentTag)) {
            if(temp.isNotEmpty()) dtCollector.add(temp)
            temp = StringBuilder()
            continue
        }
        else temp.append(item.word)
    }

    // 2. DT 처리 결과를 ItemDate에 넘겨줌(텍스트가 출현한 인덱스는 아직 고려하지 않았음)
    for(i in dtCollector){
        println("i : $i")

        var resultRegex = ymd.find(i)

        while(resultRegex != null) {
            val itemDate = ItemDate()
            var (num1, num2, num3) = resultRegex.destructured
            num1 = num1.replace(extNum, "")
            num2 = num2.replace(extNum, "")
            num3 = num3.replace(extNum, "")
            println("num1 = $num1, num2 = $num2, num3 = $num3")
            itemDate.day = num3.toInt()

            if (num1 != "" && num2 == "") itemDate.month = num1.toInt()
            else itemDate.month = num2.toIntOrNull()

            if (num1 != "" && num2 != "") itemDate.year = num1.toIntOrNull()

            itemDate.range = resultRegex.range
            println(itemDate)

            resultRegex = resultRegex.next()
        }

    }


}

fun convertTagStringToInt(s: String): Int {
    when(s){
        "O" -> return Tags.O
        "DT_YEAR" -> return Tags.DT_YEAR
        "DT_MONTH" -> return Tags.DT_MONTH
        "DT_DAY" -> return Tags.DT_DAY
        "DT_OTHERS" -> return Tags.DT_OTHERS

        "TI_DURATION" -> return Tags.TI_DURATION
    }
    return 0
}
fun isTagDT(tag: Int): Boolean {
    if(tag in 1..5) return true
    return false
}