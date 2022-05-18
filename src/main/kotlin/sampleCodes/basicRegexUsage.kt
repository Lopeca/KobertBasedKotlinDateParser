package sampleCodes

fun main(){
    val regex1 = Regex(".*(\\d?\\d?\\d\\d)[\\./년]?\\s?(\\d?\\d)[\\./월]\\s?(\\d?\\d)[^-~]?")

    val sen2 = "2015년 6월2일   2015년 6월3일"
    val sen3 = "2019. 5. 7&"
    val sen4 = "2019.10.1.()"
    val sen5 = "10.1."



    println("match(regex2): ${regex1.containsMatchIn(sen2)}")
    println("match(regex3): ${regex1.containsMatchIn(sen3)}")
    var res = regex1.find(sen3)
    if(res != null) println("${res.value} || ${res.range}")
    println("match(regex4): ${regex1.containsMatchIn(sen4)}")
    res = regex1.find(sen4)
    if(res != null) println("${res.value} || ${res.range}")
    println("match(regex5): ${regex1.containsMatchIn(sen5)}")

    val regex = "(.+)/(.+)\\.(.+)".toRegex()
    val path = "folder1/folder2/folder3/filename.java"

    if (path.matches(regex)){
        println("match!")
        val res = regex.matchEntire(path)
        if (res != null){
            val (p, n, e) = res.destructured
            println("res : $res")
            println("resss : ")
            println("$p, $n, $e")
        }
    }

    else println("not match..")



}