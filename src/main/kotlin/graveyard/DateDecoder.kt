import graveyard.DivideMarkCounter
import graveyard.TimeBox
import tools.TypeOfRegex
import java.util.regex.Pattern

fun main(){
    val sentence: MutableList<TaggedWord> = mutableListOf()
    val sampleSentence = "2015.9.8."

    tagToSentenceAI(sampleSentence)  //여기서 sentence 완성됨. 원래 활용은 sentence = tagToSentence(sampleSentence)가 된다

    // sentence가 임의로 완성됐다고 가정
    val word = TaggedWord("2015.9.8.(화)+e", Tags.DT_OTHERS)
    sentence.add(word)

    var result= TimeBox()
    if (isTagDetected(sentence,Tags.DT_DAY) != null|| isTagDetected(sentence,Tags.DT_OTHERS) != null) {
        result = decodeSentence(sentence)
    }
    println("시작 : ${result.startTime}")
    println("끝 : ${result.endTime}")
}

fun tagToSentenceAI(sampleSentence: String) {
    // 여기에 인공지능 학습한 함수 모델이 들어온다고 가정
}

fun decodeSentence(sentence: MutableList<TaggedWord>): TimeBox {
    val it = sentence.listIterator()
    val result = TimeBox()
    val decomposedSentence:MutableList<TaggedWord> = mutableListOf()

    //분해해야할 DT_OTHERS 가 있으면 분해하고 sentence 에 재합류
    while(it.hasNext()){
        println("반복중")
        val wordSet = it.next()
        println("wordSet : $wordSet")
        if(wordSet.tag == Tags.DT_OTHERS) {
            val decodedDateOthers:MutableList<TaggedWord> = decodeDateOthers(wordSet)
           decomposedSentence.addAll(decodedDateOthers)
        }
        else
            decomposedSentence.add(wordSet)
    }

    println("분해 결과 : $decomposedSentence")
    val mark = detectDivideMark(decomposedSentence)

    
    return result
}

fun detectDivideMark(decomposedSentence: MutableList<TaggedWord>):String? {
    var mark: String? = null

    // DT 년월 태그는 나눔표시를 반드시 갖고 있음. 년은 있는데 월은 없다면
    for(tag in Tags.DT_YEAR..Tags.DT_DAY) {
        if(isTagDetected(decomposedSentence,tag))
            mark = decodeDivider(decomposedSentence,tag)
        if(mark != null) return mark!!
    }
    if (isTagDetected(decomposedSentence,Tags.DT_DIVIDE)){
        val markCollector:MutableList<DivideMarkCounter> = mutableListOf()

        //divide 기호들 수집
        val it = decomposedSentence.iterator()
        while(it.hasNext()){
            val word = it.next()
            if(word.tag == Tags.DT_DIVIDE){
                if(markCollector.isEmpty()) markCollector.add(DivideMarkCounter(word.word))
                else{
                    val markIterator = markCollector.iterator()
                    var tagExist = false
                    while(markIterator.hasNext()){
                        val divideMark = markIterator.next()
                        if(word.word == divideMark.mark) {
                            tagExist = true
                            divideMark.count++
                        }
                    }
                    if(tagExist) continue
                    markCollector.add(DivideMarkCounter(word.word))
                }
            }
        }
        println(markCollector)

        markCollector.sortBy { it.count }
        markCollector.reverse()
    }
    return mark
}

fun isTagDetected(decomposedSentence: MutableList<TaggedWord>, tag: Int): Boolean {
    val it = decomposedSentence.iterator()
    var mark:String? = null
    while(it.hasNext()){
        val word = it.next()
        if(word.tag == tag) return true
    }
    return false
}

fun decodeDivider(decomposedSentence: MutableList<TaggedWord>, tag: Int): String? {
    val it = decomposedSentence.iterator()
    var mark:String? = null
    while(it.hasNext()){
        val word = it.next()
        if(word.tag == tag) mark = findDivider(word.word)
    }
    return mark
}

fun findDivider(word: String): String? {
    var prevIsNumber = false
    var splitIndex = 0

    for (i in word.indices) {
        var isNumber = Pattern.matches(TypeOfRegex.num, word[i].toString())
        if (i == 0) {
            prevIsNumber = isNumber
            continue
        }
        // 숫자였다가 아니거나 숫자가 아니었는데 숫자가 되면 앞과 분할. 분할이 목적이라서 태그는 일단 미지정
        if (!isNumber && prevIsNumber)
            splitIndex = i
    }
    if(splitIndex == 0) return null
    return word.substring(splitIndex,word.length)
}



fun decodeDateOthers(wordSet: TaggedWord): MutableList<TaggedWord> {
    val taggedWordStorage:MutableList<TaggedWord> = mutableListOf()
    var remixToSentence = StringBuilder()
    val word = wordSet.word

    var i = 0
    var splitIndex = 0
    var prevIsNumber:Boolean = false
    
    //들어온 DT_OTHERS 단어의 문자를 순서대로 보면서 공백 띄워놓기
    while(i<word.length){
        // 현재 보고 있는 문자가 숫자인지 아닌지 판정
        var isNumber = Pattern.matches(TypeOfRegex.num, word[i].toString())
        // 첫 문자는 비교대상이 없어서 넘어감
        if(i==0){
            prevIsNumber = isNumber
            i++
            continue
        }
        // 숫자였다가 아니거나 숫자가 아니었는데 숫자가 되면 앞과 분할. 분할이 목적이라서 태그는 일단 미지정
        if(isNumber != prevIsNumber){
            val piece = word.substring(splitIndex until i)
            remixToSentence.append("$piece ")
            splitIndex = i
        }

        // 비숫자에서 비숫자로 왔지만 2칸 앞에가 숫자면 여기서 분할('부터' 와 '까지' 등 2문자 이상의 약속표현에 예외 처리가 필요할 부분)
        else if (i > 1 && !isNumber && Pattern.matches(TypeOfRegex.num, word[i-2].toString())) {
            val piece = word.substring(splitIndex until i)
            remixToSentence.append("$piece ")
            splitIndex = i
        }
        prevIsNumber = isNumber
        i++
    }

    // 마지막 토막
    val piece = word.substring(splitIndex until i)
    remixToSentence.append(piece)

    println("재조합한 문장 : ${remixToSentence.toString()}")
    // 재조합한 문장에 다시 태깅작업. 역시 MutableList<TaggedWord> 반환이 원래 있어야 함
    // tagToSentenceAI(remixToSentence.toString())

    // TODO : 띄어쓰기를 해두면 DT취급은 하는데 OTHERS로 전부 분류된다. 유료결제로 다량데이터 학습한 모델에서 어떨지 알아봐야함
    // 위 인공지능 함수를 안 쓸 때 아래 정규표현식 해독기
    taggedWordStorage.addAll(decodeDateOthersByRegex(remixToSentence.toString()))

    return taggedWordStorage
}

fun decodeDateOthersByRegex(arranged: String): MutableList<TaggedWord> {
    val wordBox = arranged.split(" ").toTypedArray()
    val taggedWordBox:MutableList<TaggedWord> = mutableListOf()

    // 숫자 항은 일단 DT_UNDEFINED, 물결표는 DURATION
    for (word in wordBox){
        if(Pattern.matches(TypeOfRegex.num,word)) taggedWordBox.add(TaggedWord(word,Tags.DT_UNDEFINED))
        else if (Pattern.matches(TypeOfRegex.duration,word))
        else taggedWordBox.add(TaggedWord(word,Tags.O))
    }

    // 숫자 바로 뒤에 쫓아오는 문자 속성을 판정하는 부분. 여기서 사회적 약속(월, 일)등을 특수 처리할 수 있음
    for(i in 1 until wordBox.size-1){
        if (taggedWordBox[i-1].tag == Tags.DT_UNDEFINED) {
            if (!candidateToDivide(wordBox[i])) continue

            taggedWordBox[i].tag = Tags.DT_DIVIDE
        }
    }
    println(taggedWordBox)
    return taggedWordBox
}


fun candidateToDivide(s: String): Boolean {
    if(s.length != 1) return false
    return Pattern.matches(TypeOfRegex.candidateDivider,s)
}

fun candidateDuration(s: String): Boolean {
    if(s.length != 1) return false
    return Pattern.matches(TypeOfRegex.duration,s)
}

