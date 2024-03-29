import com.google.gson.GsonBuilder
import scheduleItem.*
import tools.*
import java.io.File
import java.util.*

fun main(){

//
//    // 정규표현식
    val ymd = Regex("\\D*(\\d*[\\./년\\s])?\\s?(\\d*[\\./월\\s])?\\s?(\\d+)[^-~]?")


    // 지금 커밋한 데이터는 ner_train_data(17)의 문장입니다
    // 아래 taggedWords가 완성된 시점까지는 학습해서 데이터 받아올 때 필요 없을 수 있는 코드
    // --------연습 문장 original***-------------
//    val originalSentence = "내일 건축가 <건축가 명사특강> 2019.8.27 -9.24 어디서 살 것인가 9.3 180* 매주 (화) 16:00 - 18:00 세종시청 4층 여민실 정태인 |"
//    val originalTag = "DT_OTHERS O O O DT_OTHERS DT_OTHERS O O O DT_OTHERS O DT_OTHERS DT_OTHERS TL_OTHERS O TL_OTHERS O O O O O"

//    val originalSentence = "2019 9월 내일 건축가 <건축가 명사특강> 2019.8.27 -9.24 어디서 살 것인가 9시간뒤 180* 매주 (화) 16:00 - 18:00 세종시청 4층 여민실 정태인 |"
//    val originalTag = "DT_YEAR DT_MONTH DT_OTHERS O O O DT_OTHERS DT_OTHERS O O O DT_OTHERS O DT_OTHERS DT_OTHERS TI_HOUR O TI_HOUR O O O O O"

    val originalSentence = "16:00 - 18:00"
    val originalTag = "TI_DURATION"


    //val randomItemTimeSingle = ItemTime(5,0,1..1)

    // 연습문장을 띄어쓰기로 구분해서 넣는 코드를 주로 썼던 부분입니다. original 에서 연습 문장 쓸 때 위에 태그 붙이는 거랑 이거랑 잘 맞춰야해요
    val beforeWords:List<String> = arrayListOf(originalSentence)
    //val beforeWords = originalSentence.split(" ")
    val beforeTags = originalTag.split(" ")

    println(beforeWords.size)
    println(beforeTags.size)

    val taggedWords:MutableList<TaggedWord> = mutableListOf()
    val itemDateList:MutableList<ItemDate> = mutableListOf()
    val itemTimeList:MutableList<ItemTime> = mutableListOf()

    val PT = ProcessTime()
    var timeObjects: MutableList<ItemTime>? = null

    for(i in beforeWords.indices){
        val currentTag:Int = convertTagStringToInt(beforeTags[i])
        taggedWords.add(TaggedWord(beforeWords[i], currentTag))

        //시간은 여기서 아이템화
        if(Regex("^TI").find(beforeTags[i]) != null)
            timeObjects = PT.sepTime(beforeWords[i], beforeTags[i])

        if(timeObjects == null) continue

        for (j in timeObjects) {
            j.range = i..i
        }
        println("TimeOb : $timeObjects")
        itemTimeList.addAll(timeObjects)
        timeObjects.clear()
    }
    println(itemTimeList)

    // 여기까지 인공지능 받을 때 달라질 코드



    // 0. DT나 TI 태그에서 "후/뒤" 글자 발견되면 태그 변환
    for(w in taggedWords){
        if(isTagDT(w.tag) || isTagTI(w.tag)) {
            var result = Regex("[후|뒤]").find(w.word)
            if (result != null) {
                println("단어 : ${w.word}")
                w.tag = Tags.DTI_RESERVATION
            }
        }
    }

    // 1. DT 태그가 연속해서 출현하면 붙여서 저장. StringPositionRecorder 리스트 사용
    println("---------------1단계--------------")
    val rangedWordBox:MutableList<StringPositionRecorder> = packUpWords(taggedWords)


    // 2. DT 태그를 연결시켜 재정리한 결과물인 StringPositionRecorder를 정규표현식으로 분석해서 ItemDate에 넘겨줌
    println("---------------2단계--------------")

    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_MONTH)
    for(i in rangedWordBox){
            println("i : $i")
            // 정규표현식 적용하는 부분
            var resultRegexDate = ymd.find(i.str)
            if(resultRegexDate != null) {
                while (resultRegexDate != null) {
                    val itemDate: ItemDate? = parseDateByRegex(i, resultRegexDate)
                    if(itemDate != null) itemDateList.add(itemDate)
                    resultRegexDate = resultRegexDate.next()
                }
            }
            else{
                var thisRange = if(i.endIndex != null) i.startIndex..i.endIndex!! else i.startIndex..i.startIndex
                var result = Regex("내일").find(i.str)
                if(result != null) itemDateList.add(ItemDate(null,null,today+1,thisRange))
                else {
                    result = Regex("모레").find(i.str)
                    if(result != null) itemDateList.add(ItemDate(null,null,today+2,thisRange))
                    else {
                        result = Regex("어제").find(i.str)
                        if(result != null) itemDateList.add(ItemDate(null,null,today-1,thisRange))
                    }
                }
            }
    }

    // 일정과 시간 모두 모였다고 가정하고 어떻게 처리할지?


    // 3. 일정 사이드부터 수집. dt리스트는 itemDateList, ti리스트는 itemTimeList
    println("---------------3단계--------------")
   // itemTimeList.add(randomItemTimeSingle)  // 처리할 시간이 있다고 임의로 만들어서 가정

    val itemSideList:MutableList<ItemSide> = mutableListOf()
    // DT_RESERVATION 에서도 아이템 사이드 추출해서 리스트에 추가
    parseItemSideFromDateReservation(taggedWords, itemDateList, itemSideList)
    println(itemDateList)
    println(itemTimeList)

    // 일자와 인접한 시간은 일자와 묶어서 itemSide로 들어감
    for(dateItem in itemDateList){
        val sideItem = ItemSide(dateItem,range=dateItem.range!!)
        for(timeItem in itemTimeList){
            // 인식된 시간일정의 멀지 않은 앞쪽에 인식된 날짜일정이 있으면 년월일-시/분 연결된 하나의 일정으로 취급
            if(timeItem.range!!.first - dateItem.range!!.last in 1..2) {
                sideItem.itemTime = timeItem
                sideItem.range = dateItem.range!!.first..timeItem.range!!.last
                timeItem.inserted = true
                break
            }
        }
        itemSideList.add(sideItem)
    }

    // 들어가지 못한 시간은 시간만 인식된 독립된 일정으로 처리.
    for(timeItem in itemTimeList){
        if(!timeItem.inserted) {
            itemSideList.add(ItemSide(null,timeItem,timeItem.range!!))
        }
    }


    itemSideList.sortBy { it.range.first }
    println(itemSideList)

    // 4. itemSideList에서 ItemSide끼리 시작과 종료 날짜로 관련됐는지, 각기 무관한 일정인지 출신 인덱스 범위로 분석해서 일정 아이템화(작업중)
    val scheduleList:MutableList<ItemSchedule> = mutableListOf()
    var tempSchedule:ItemSchedule? = null
    var prevRangeStart = 0
    var prevRangeEnd = 0
    for(item in itemSideList){
        if(tempSchedule == null){
            tempSchedule = ItemSchedule(item, null, item.range)
        }
        else{
            if(item.range.first - prevRangeEnd <= 2 && item.range.first >= prevRangeStart){
                tempSchedule.to = item
                tempSchedule.range = tempSchedule.range.first..item.range.last
                scheduleList.add(tempSchedule)
                tempSchedule = null
            }
            else{
                scheduleList.add(tempSchedule)
                tempSchedule = ItemSchedule(item, null, item.range)
            }
        }
        prevRangeStart = item.range.first
        prevRangeEnd = item.range.last
    }
    if(tempSchedule!=null) scheduleList.add(tempSchedule)
    println(scheduleList)

    val eventList:MutableList<EventsVO> = convertListScheduleToEvent(scheduleList)

    println("----------------------------event----------------------------")
    for(event in eventList){
        val from = Date(event.dtStart)
        var to:Date = if(event.dtEnd != null) Date(event.dtEnd!!)
        else Date(event.dtStart+60000*60)

        val fromP = DateForm.integratedForm.format(event.dtStart)
        val toP = DateForm.integratedForm.format(event.dtEnd!!)
        println("$fromP ~ $toP")
    }
}



fun parseItemSideFromDateReservation(reservationWords: MutableList<TaggedWord>, itemDateList:MutableList<ItemDate>, itemSideList: MutableList<ItemSide>) {
    var idx:Int = -1
    for (word in reservationWords){
        idx++
        if(word.tag!=Tags.DTI_RESERVATION) continue
        println("[[$word]]")
        val cal = Calendar.getInstance()

        println("${cal.get(Calendar.HOUR_OF_DAY)} : ${cal.get(Calendar.MINUTE)}")
        val seperatedPromised:ArrayList<PromisedExpressionSet> = dividePromisedSentence(word.word)
        var isTimeReservationDetected = false
        // TODO : cal에 예약일정 추가
        for (p in seperatedPromised){
            println("p : ${p.expression}")
            when(p.roleTag){
                PromisedTags.YEAR -> cal.add(Calendar.YEAR, p.amount)
                PromisedTags.MONTH -> cal.add(Calendar.MONTH, p.amount)
                PromisedTags.DAY -> cal.add(Calendar.DAY_OF_MONTH, p.amount)
                PromisedTags.HOUR -> {
                    isTimeReservationDetected = true
                    cal.add(Calendar.HOUR, p.amount)
                }
                PromisedTags.MINUTE -> {
                    isTimeReservationDetected = true
                    cal.add(Calendar.MINUTE, p.amount)
                }
            }
        }

        println("${cal.get(Calendar.HOUR_OF_DAY)} : ${cal.get(Calendar.MINUTE)}")

        val curDate = ItemDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH), idx..idx)
        if(!isTimeReservationDetected) {        //예약일정이 날짜만 다루고 시간까지 안 다루면 ItemDate 리스트에 넣고 스킵
            println(curDate)
            itemDateList.add(curDate)
            continue
        }

        val curTime = ItemTime(cal.get(Calendar.HOUR), cal.get(Calendar.MONTH))
        itemSideList.add(ItemSide(curDate, curTime, idx..idx))
    }
}

fun dividePromisedSentence(word: String): ArrayList<PromisedExpressionSet> {
    val temp:ArrayList<String> = arrayListOf()
    val frag = StringBuilder()
    var prevIsNumber = false
    var result:ArrayList<PromisedExpressionSet> = arrayListOf()

    // 문자였다가 숫자가 되는 순간을 나눠서 저장
    for(i in word.indices){
        if(i==0) {
            frag.append(word[i])
            prevIsNumber = TypeOfRegex.isNum.find(word[i].toString()) != null
            continue
        }
        if(!prevIsNumber && TypeOfRegex.isNum.find(word[i].toString()) != null){
            temp.add(frag.toString())
            frag.setLength(0)
        }
        frag.append(word[i])
        prevIsNumber = TypeOfRegex.isNum.find(word[i].toString()) != null
    }
    temp.add(frag.toString())

    //TODO : 숫자와 문자를 분리해 PES 에 저장하고 PES 분석 돌려서 리스트에 넣어 리턴
    for (i in temp){
        val ps = PromisedExpressionSet()
        ps.amount = i.replace("\\D".toRegex(), "").toInt()
        ps.expression = i.replace("\\d".toRegex(), "")
        ps.decodeRole()
        result.add(ps)
    }


    return result
}

fun fillNullDefaultItemSchedule(itemSch: ItemSchedule) {
    fillNullDefaultItemSide(itemSch.from)
    if(itemSch.to != null) {
        println("asdf : $itemSch.to")
        if(itemSch.to!!.itemDate == null) itemSch.to!!.itemDate = itemSch.from.itemDate!!.copy()
        else {
            if(itemSch.to!!.itemDate!!.year == null) itemSch.to!!.itemDate!!.year = itemSch.from.itemDate!!.year
            if(itemSch.to!!.itemDate!!.month == null) itemSch.to!!.itemDate!!.month = itemSch.from.itemDate!!.month
        }

        if(itemSch.to!!.itemTime == null) fillNullDefaultItemTime(itemSch.to!!.itemTime)
    }

}

fun fillNullDefaultItemSide(side: ItemSide) {
    side.itemDate = fillNullDefaultItemDate(side.itemDate)
    side.itemTime = fillNullDefaultItemTime(side.itemTime)
}

fun fillNullDefaultItemTime(itemTime: ItemTime?): ItemTime? {
    var finishedTime = itemTime
    if(finishedTime == null){
        finishedTime = ItemTime(9, 0)
    }
    else{
        if(finishedTime.minute == null) finishedTime.minute = 0
    }
    return finishedTime
}

fun fillNullDefaultItemDate(itemDate: ItemDate?): ItemDate? {
    val cal = Calendar.getInstance()
    val curYear = cal.get(Calendar.YEAR)
    val curMonth = cal.get(Calendar.MONTH)+1
    val curDay = cal.get(Calendar.DAY_OF_MONTH)

    var finishedDate = itemDate
    if(finishedDate == null){
        finishedDate = ItemDate(curYear, curMonth, curDay)
    }
    else{
        if(finishedDate.year == null) finishedDate.year = curYear
        if(finishedDate.month == null)finishedDate.month = curMonth
        if(finishedDate.day == null) finishedDate.day = curDay
    }

    return finishedDate
}

fun convertSideToMillis(side: ItemSide): Long {
    val cal = Calendar.getInstance()

    println("여기 확인 : $side")
    val year = side.itemDate!!.year!!
    val month = side.itemDate!!.month!!-1
    val day = side.itemDate!!.day!!
    if(side.itemTime != null) {
        val hour = side.itemTime!!.hour!!
        val minute = side.itemTime!!.minute!!

        println("::$year, $month,$day,$hour,$minute")
        cal.set(year, month, day, hour, minute)
    }
    else
        cal.set(year,month,day, 9, 0)
    return cal.timeInMillis
}

fun convertListScheduleToEvent(scheduleList: MutableList<ItemSchedule>): MutableList<EventsVO> {
    val eventList:MutableList<EventsVO> = mutableListOf()

    for(itemSch in scheduleList){
        fillNullDefaultItemSchedule(itemSch)

        val fromMillis = convertSideToMillis(itemSch.from)
        var toMillis:Long = if (itemSch.to == null) fromMillis+60000*60 else convertSideToMillis(itemSch.to!!)
        val eventTemp = EventsVO(0,1,null,"회의",null,null, 1,1,fromMillis,toMillis,"Asia/Seoul",null,null,null,null,null,null,null)
        eventList.add(eventTemp)
    }

    return eventList
}

fun detectedKorButDate(value: String): Boolean {
    println("한글확인 + $value")
    val findKor = Regex("년|월").find(value)
    if(findKor != null) {
        Regex("일").find(value) ?: return false
    }
    return true
}

fun parseDateByRegex(i:StringPositionRecorder, resultRegexDate: MatchResult): ItemDate? {
    // 날짜 정보가 들어있으면 ItemDate로 가공
    val itemDate = ItemDate()
    println("문장 : " + resultRegexDate.value)

    //년, 월이 있는데 일이 없으면 탈락
    if(!detectedKorButDate(resultRegexDate.value)) return null
    var (n1, n2, n3) = resultRegexDate.destructured
    var num1 = n1.replace(TypeOfRegex.extNum, "")
    var num2 = n2.replace(TypeOfRegex.extNum, "")
    var num3 = n3.replace(TypeOfRegex.extNum, "")
    println("num1 = $num1, num2 = $num2, num3 = $num3")

    // divider가 다르면 앞에것만 취함
    if(num1.isNotEmpty() && num2.isNotEmpty()) {
        if (n1[num1.length] != '년' && n1[num1.length] != n2[num2.length]) {
            num3 = num2
            num2 = num1
            num1 = ""
        }
    }

    // num3부터 일.월.년 순으로 취급함
    itemDate.day = num3.toInt()

    if (num1 != "" && num2 == "") itemDate.month = num1.toInt()
    else itemDate.month = num2.toIntOrNull()

    if (num1 != "" && num2 != "") itemDate.year = num1.toIntOrNull()

    // 이 일정이 추출된 단어의 원본에서의 구간 저장
    if(i.endIndex !=null) itemDate.range = i.startIndex..i.endIndex!!
    else itemDate.range = i.startIndex..i.startIndex
    println(itemDate)

    if(itemDate.year!=null) {
        if (itemDate.year!! < 2000 || itemDate.year!!>2200)
        return null
    }
    if(itemDate.month!=null) if (itemDate.month!! >12) return null
    if(itemDate.day!! >31) return null
    return itemDate
}

fun packUpWords(taggedWords: MutableList<TaggedWord>): MutableList<StringPositionRecorder> {
    val wordPacks:MutableList<StringPositionRecorder> = mutableListOf()
    var temp:StringPositionRecorder? = null
    for ((index, item) in taggedWords.withIndex()) {
        val currentTag = item.tag
        if (!isTagDT(currentTag)) {
            if(temp!=null) {
                temp.endIndex = index-1
                wordPacks.add(temp)
                temp = null
            }
            continue
        }
        else {
            if(temp == null) temp = StringPositionRecorder(startIndex = index)
            temp.str.append(item.word)
        }
    }
    println(wordPacks)
    return(wordPacks)
}

fun convertTagStringToInt(s: String): Int {
    when(s){
        "O" -> return Tags.O
        "DT_YEAR" -> return Tags.DT_YEAR
        "DT_MONTH" -> return Tags.DT_MONTH
        "DT_DAY" -> return Tags.DT_DAY
        "DT_OTHERS" -> return Tags.DT_OTHERS
        "QT_OTHERS" -> return Tags.DT_OTHERS
        "QT_ORDER" -> return Tags.DT_OTHERS
        "QT_PERCENTAGE" -> return Tags.DT_OTHERS

        "TI_DURATION" -> return Tags.TI_DURATION
    }
    return 0
}
fun isTagDT(tag: Int): Boolean {
    if(tag in 1..5) return true
    return false
}

fun isTagTI(tag: Int): Boolean {
    if(tag in 11..14) return true
    return false
}