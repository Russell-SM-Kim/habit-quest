package com.lifereset.core.game.rules

/** 레벨 구간별 랭크. 레벨업 보너스 스탯 포인트와 랭크업 칭호를 정한다. */
enum class Rank(val minLevel: Int, val bonusPoints: Int, val title: String) {
    E(1, 5, "각성자"),
    D(10, 6, "수련생"),
    C(20, 7, "숙련자"),
    B(35, 8, "정예"),
    A(50, 9, "강자"),
    S(70, 10, "초월자"),
    ;

    companion object {
        fun of(level: Int): Rank = entries.last { level >= it.minLevel }

        /** S 랭크 이후에는 10레벨마다 별이 붙는다: S, S★1, S★2 … */
        fun label(level: Int): String {
            val rank = of(level)
            if (rank != S) return rank.name
            val stars = (level - S.minLevel) / 10
            return if (stars == 0) "S" else "S★$stars"
        }
    }
}
