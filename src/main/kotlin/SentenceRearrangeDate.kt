import scheduleItem.ItemDate
import scheduleItem.ItemSchedule
import scheduleItem.ItemSide
import scheduleItem.ItemTime
import tools.StringPositionRecorder

fun main(){
    // 정규표현식
    val ymd = Regex("\\D*(\\d*[\\./년\\s])?\\s?(\\d*[\\./월\\s])?\\s?(\\d+)[^-~]?")
    val extNum = Regex("\\D")

    // 지금 커밋한 데이터는 ner_train_data(17)의 문장입니다
    // 아래 taggedWords가 완성된 시점까지는 학습해서 데이터 받아올 때 필요 없을 수 있는 코드
    val originalSentence = "건축가 <건축가 명사특강> 2019.8.27 -9.24 어디서 살 것인가 9.3 180* 매주 (화) 16:00 - 18:00 세종시청 4층 여민실 정태인 |"
    val originalTag = "O O O DT_OTHERS DT_OTHERS O O O DT_OTHERS O DT_OTHERS DT_OTHERS TL_OTHERS O TL_OTHERS O O O O O"

    val randomItemTimeSingle = ItemTime(16,0,13..13)

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
    // 1. DT 태그가 연속해서 출현하면 붙여서 저장
    println("---------------1단계--------------")
    val dtCollector:MutableList<StringPositionRecorder> = mutableListOf()
    var temp:StringPositionRecorder? = null
    for ((index, item) in taggedWords.withIndex()) {
        val currentTag = item.tag
        if (!isTagDT(currentTag)) {
            if(temp!=null) {
                temp.endIndex = index-1
                dtCollector.add(temp)
                temp = null
            }
            continue
        }
        else {
            if(temp == null) temp = StringPositionRecorder(startIndex = index)
            temp.str.append(item.word)
        }
    }

    println(dtCollector)

    // 2. DT 처리 결과를 ItemDate에 넘겨줌(텍스트가 출현한 인덱스는 아직 고려하지 않았음)
    println("---------------2단계--------------")
    val itemDateList:MutableList<ItemDate> = mutableListOf()
    val itemTimeList:MutableList<ItemTime> = mutableListOf()
    for(i in dtCollector){
        println("i : $i")
        var resultRegex = ymd.find(i.str)

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

            if(i.endIndex !=null) itemDate.range = i.startIndex..i.endIndex!!
            else itemDate.range = i.startIndex..i.startIndex
            println(itemDate)
            itemDateList.add(itemDate)
            resultRegex = resultRegex.next()
        }
    }

    // 일정과 시간 모두 모였다고 가정하고 어떻게 처리할지?
    // 3. 일정 사이드부터 수집. dt리스트는 itemDateList, ti리스트는 itemTimeList
    println("---------------3단계--------------")
    itemTimeList.add(randomItemTimeSingle)

    val itemSideList:MutableList<ItemSide> = mutableListOf()

    // 일자와 인접한 시간은 일자와 묶어서 itemSide로 들어감
    for(dateItem in itemDateList){
        val sideItem = ItemSide(dateItem,range=dateItem.range!!)
        for(timeItem in itemTimeList){
            if(timeItem.range!!.first - dateItem.range!!.last in 1..2) {
                sideItem.itemTime = timeItem
                sideItem.range = dateItem.range!!.first..timeItem.range!!.last
                timeItem.inserted = true
                break
            }
        }
        itemSideList.add(sideItem)
    }

    //들어가지 못한 시간은 별도의 일정일 가능성이 있음
    for(timeItem in itemTimeList){
        if(!timeItem.inserted) {
            itemSideList.add(ItemSide(null,timeItem,timeItem.range!!))
        }
    }

    println(itemSideList)

    // 4. itemSideList에서 시작과 종료 날짜로 관련됐는지, 각기 다른 일정인지 인덱스 범위로 분석해서 일정 아이템화
    val scheduleList:MutableList<ItemSchedule> = mutableListOf()
    var tempSchedule:ItemSchedule? = null
    var prevRangeEnd = 0
    for(item in itemSideList){
        if(tempSchedule == null){
            tempSchedule = ItemSchedule(item, null, item.range)
        }

        prevRangeEnd = item.range.last

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