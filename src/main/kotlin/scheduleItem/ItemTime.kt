package scheduleItem

data class ItemTime(var hour:Int?=null, val minute:Int?=null, var range:IntRange? = null, var inserted:Boolean = false)
