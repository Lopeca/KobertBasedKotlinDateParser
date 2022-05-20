fun main(){
    // 지금 커밋한 데이터는 ner_train_data(17)의 문장입니다
    val originalSentence = "오 오~ KICKSTA-TEK 초청강연 미국의 대표적인 크라우드 펀딩 서비스 킥스타터 JCT 일시 2019. 12. 06. (2) 16:30-17:30 장소 인하대학교 현경홀(구 중강당)"
    val originalTag = "O O O O O O O O O O O O DT_YEAR DT_MONTH DT_DAY O TI_DURATION O O O O"

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

    val dtCollector = StringBuilder()

    for (item in taggedWords) {
        val currentTag = item.tag
        if (!isTagDT(currentTag)) continue
        dtCollector.append(item.word)
    }

    println(dtCollector.toString())
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