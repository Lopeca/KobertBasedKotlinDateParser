package scheduleItem

data class ItemSchedule(val from:ItemSide, val to:ItemSide? = null, val range:IntRange)
