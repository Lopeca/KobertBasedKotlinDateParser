package graveyard

import tools.PromisedExpressionSet
import tools.TypeOfRegex
import java.util.ArrayList

fun main(){
    val word = "3개월 5일 후"

    val temp: ArrayList<String> = arrayListOf()
    val frag = StringBuilder()
    var prevIsNumber = false
    var result: ArrayList<PromisedExpressionSet> = arrayListOf()

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
    println(temp)

    for (i in temp){
        val ps = PromisedExpressionSet()
        ps.amount = i.replace("\\D".toRegex(), "").toInt()
        ps.expression = i.replace("\\d".toRegex(), "")
        ps.decodeRole()
        ps.print()
    }


}