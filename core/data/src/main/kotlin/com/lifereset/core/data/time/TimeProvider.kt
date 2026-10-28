package com.lifereset.core.data.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** 시계는 여기 한 곳에서만 읽는다 (테스트에서 바꿔 끼울 수 있게). */
interface TimeProvider {
    fun nowMillis(): Long
    fun today(): LocalDate

    /** 날짜가 바뀔 때마다 값을 낸다 (자정에 일일 퀘스트 갱신). */
    val todayFlow: Flow<LocalDate>
}

class SystemTimeProvider(private val zone: () -> ZoneId = { ZoneId.systemDefault() }) : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now(zone())

    override val todayFlow: Flow<LocalDate> = flow {
        while (true) {
            emit(today())
            val now = ZonedDateTime.now(zone())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone())
            val waitMillis = java.time.Duration.between(now, nextMidnight).toMillis()
            // 기기 시간이 바뀌는 경우를 대비해 최대 1분마다 다시 확인한다
            delay(waitMillis.coerceIn(1_000L, 60_000L))
        }
    }.distinctUntilChanged()
}
