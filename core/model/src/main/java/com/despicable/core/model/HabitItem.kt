package com.despicable.core.model


data class HabitItem(
    val id: Long = 0,
    val noteId: Long,
    val habitName: String,
    val targetDays: Int = 0,
    val completedDays: Int = 0,
    val startDate: Long,
    val lastUpdated: Long = System.currentTimeMillis(),
    val completions: List<HabitCompletion> = emptyList()
) {

    //  Calculate progress percentage
    fun progressPercentage(): Float = if (targetDays > 0) {
        (completedDays.toFloat() / targetDays) * 100
    } else 0f


    //  Check if habit is completed
    fun isCompleted(): Boolean = completedDays >= targetDays


    //  Check if habit was completed on a specific date
    fun isCompletedOnDate(date: Long): Boolean =
        completions.any { it.date == date && it.isCompleted }
}


data class HabitCompletion(
    val id: Long = 0,
    val habitId: Long,
    val date: Long,
    val isCompleted: Boolean
)