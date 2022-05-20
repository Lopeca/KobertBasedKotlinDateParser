package sampleCodes

fun main(){
    val ymd = Regex("\\D*(\\d*[\\./년\\s])?\\s?(\\d*[\\./월\\s])?\\s?(\\d+)[^-~]?")

    val sen1 = "2015.9.8.(화)+e"
    val sen2 = "6월2일   2015년 6월3일"
    val sen3 = "1월 5일"
    val sen4 = "     2019.10.1.()"
    val sen5 = " 2"

    var resYmd = ymd.find(sen1)
    printDestructured(resYmd)
    resYmd = ymd.find(sen2)
    printDestructured(resYmd)
    resYmd = resYmd?.next()
    printDestructured(resYmd)
    resYmd = ymd.find(sen3)
    printDestructured(resYmd)
    resYmd = ymd.find(sen4)
    printDestructured(resYmd)
    resYmd = ymd.find(sen5)
    printDestructured(resYmd)

    resYmd = ymd.find(sen4)
    while(resYmd != null){
        val(year, month, day) = resYmd.destructured

        println(sen4)
        println("year : $year || month : $month || day : $day")
        println(resYmd.range)

        resYmd = resYmd.next()
    }
}

fun printDestructured(res:MatchResult?){
    if(res!=null)
        println(res.destructured.toList())
}
