package com.lifereset.core.game.rules

/**
 * 맨몸 근력 서킷 단계표. 2~3주마다 목표가 오르고, 4단계 이후에도 3주마다 계속 오른다.
 * 헬스장 기능을 붙일 때는 같은 방식으로 새 프로그램을 추가한다.
 */
object StrengthProgram {
    const val ID = "bodyweight"

    data class Exercise(val name: String, val sets: Int, val reps: Int, val unit: String = "회") {
        fun format(): String = "$name ${sets}×$reps${if (unit == "초") "초" else ""}"
    }

    data class Phase(val number: Int, val exercises: List<Exercise>) {
        fun summary(): String = exercises.joinToString(" · ") { it.format() }
    }

    /** 1주차부터 시작하는 주 번호로 단계를 구한다. */
    fun phaseForWeek(week: Int): Phase {
        val number = when {
            week <= 2 -> 1
            week <= 4 -> 2
            week <= 7 -> 3
            week <= 10 -> 4
            else -> 4 + (week - 8) / 3 // 11주차부터 3주마다 +1
        }
        return phase(number)
    }

    fun phase(number: Int): Phase = when (number) {
        1 -> Phase(1, listOf(push(3, 8), squat(3, 15), plank(3, 30)))
        2 -> Phase(2, listOf(push(3, 10), squat(3, 20), plank(3, 40), lunge(2, 8)))
        3 -> Phase(3, listOf(push(3, 12), squat(3, 25), plank(3, 50), lunge(3, 10)))
        4 -> Phase(4, listOf(push(4, 12), squat(4, 25), plank(3, 60), lunge(3, 12)))
        else -> {
            val k = number - 4
            Phase(number, listOf(push(4, 12 + 2 * k), squat(4, 25 + 5 * k), plank(3, 60 + 10 * k), lunge(3, 12 + 2 * k)))
        }
    }

    private fun push(sets: Int, reps: Int) = Exercise("푸시업", sets, reps)
    private fun squat(sets: Int, reps: Int) = Exercise("스쿼트", sets, reps)
    private fun plank(sets: Int, seconds: Int) = Exercise("플랭크", sets, seconds, unit = "초")
    private fun lunge(sets: Int, reps: Int) = Exercise("런지(한쪽)", sets, reps)
}
