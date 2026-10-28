package com.lifereset.core.game.model

/** 6개 능력치. 순서(ordinal)는 동점일 때 우선순위로도 쓰이므로 함부로 바꾸지 않는다. */
enum class Stat(val code: String, val label: String) {
    STR("STR", "근력"),
    END("END", "지구력"),
    AGI("AGI", "민첩"),
    INT("INT", "지능"),
    WIL("WIL", "정신력"),
    VIT("VIT", "활력"),
}

/** 퀘스트가 어떤 스탯을 얼마나 키우는지. 값의 합은 보통 1.0 이다. */
typealias StatWeights = Map<Stat, Double>

/**
 * StatWeights <-> 문자열 변환. DB 와 통신에서 같은 형식을 쓴다.
 * 형식: "END:0.8,VIT:0.2"
 */
object StatWeightsCodec {
    fun encode(weights: StatWeights): String =
        weights.entries
            .sortedBy { it.key.ordinal }
            .joinToString(",") { "${it.key.name}:${it.value}" }

    fun decode(text: String): StatWeights {
        if (text.isBlank()) return emptyMap()
        return text.split(",").mapNotNull { part ->
            val pieces = part.trim().split(":")
            if (pieces.size != 2) return@mapNotNull null
            val stat = Stat.entries.firstOrNull { it.name == pieces[0] } ?: return@mapNotNull null
            val weight = pieces[1].toDoubleOrNull() ?: return@mapNotNull null
            stat to weight
        }.toMap()
    }
}
