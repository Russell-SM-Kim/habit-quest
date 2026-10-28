package com.lifereset.core.game.model

import java.time.DayOfWeek
import java.time.LocalDate

enum class Difficulty(val baseXp: Int, val label: String) {
    EASY(10, "쉬움"),
    NORMAL(20, "보통"),
    HARD(35, "어려움"),
    EPIC(60, "에픽"),
}

enum class QuestCategory {
    /** 생활 습관. 주간 보스에 데미지를 주지 않는다. */
    HABIT,

    /** 운동. 주간 보스에 데미지를 준다. */
    EXERCISE,
}

/** 수치 목표의 단위. 초과 달성 보너스 계산에 쓰인다. */
enum class QuestUnit(val label: String) {
    MINUTES("분"),
    KM("km"),
    REPS("회"),
}

/** "20분", "3km" 같은 수치 목표. 없으면 체크만 하는 퀘스트다. */
data class QuestTarget(val amount: Double, val unit: QuestUnit) {
    init {
        require(amount > 0) { "목표량은 0보다 커야 합니다" }
    }

    fun format(): String = "${amount.formatAmount()}${unit.label}"
}

/**
 * 퀘스트 정의. 사용자가 만들고 고칠 수 있는 '규칙'이다.
 * 완료 기록(Completion)은 완료 시점의 난이도·가중치를 따로 복사해 두므로,
 * 나중에 퀘스트를 고쳐도 과거 EXP 는 바뀌지 않는다.
 */
data class QuestDef(
    val id: String,
    val title: String,
    val difficulty: Difficulty,
    val weights: StatWeights,
    val days: Set<DayOfWeek>,
    val createdOn: LocalDate,
    val target: QuestTarget? = null,
    /** 목표 설명 (예: "07:00 전 체크"). 수치 목표가 없을 때 보여준다. */
    val note: String = "",
    val category: QuestCategory = QuestCategory.HABIT,
    /** 같은 값을 가진 퀘스트끼리는 하루에 하나만 하면 된다 (예: 탁구 또는 러닝). */
    val choiceGroup: String? = null,
    /** 승패를 기록하는 퀘스트 (탁구). 이기면 민첩 스탯 경험치 보너스. */
    val tracksWin: Boolean = false,
    /** 단계별로 목표가 오르는 프로그램 (예: 맨몸 근력). [com.lifereset.core.game.rules.StrengthProgram] 참고. */
    val programId: String? = null,
    val archivedOn: LocalDate? = null,
    val sortOrder: Int = 0,
) {
    /** 하루 슬롯 키. 택1 그룹이면 그룹 이름, 아니면 퀘스트 id. */
    val slotKey: String get() = choiceGroup ?: id

    fun isDueOn(date: LocalDate): Boolean =
        date.dayOfWeek in days &&
            !date.isBefore(createdOn) &&
            (archivedOn == null || date.isBefore(archivedOn))

    val isArchived: Boolean get() = archivedOn != null
}

/** 3.0 -> "3", 3.5 -> "3.5" */
fun Double.formatAmount(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, this)
