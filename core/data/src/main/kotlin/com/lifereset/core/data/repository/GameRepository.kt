package com.lifereset.core.data.repository

import com.lifereset.core.data.time.TimeProvider
import com.lifereset.core.game.engine.EngineInput
import com.lifereset.core.game.engine.Fx
import com.lifereset.core.game.engine.FxDiff
import com.lifereset.core.game.engine.GameEngine
import com.lifereset.core.game.model.Completion
import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.Stat
import com.lifereset.core.game.rules.GameRules
import com.lifereset.core.game.rules.RecoveryQuest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * 게임의 중심. 기록이 바뀔 때마다 엔진으로 상태를 다시 계산하고,
 * 사용자의 행동(완료·취소·기록 추가)으로 생긴 연출(Fx)을 흘려보낸다.
 * 폰 화면과 워치 요청이 모두 이 인터페이스를 통해 들어온다.
 */
interface GameRepository {
    /** null = 아직 첫 계산 전 */
    val state: StateFlow<PlayerState?>

    /** 재생할 연출. 화면 쪽 FxHost 가 순서대로 보여준다. */
    val fx: Flow<Fx>

    /**
     * 퀘스트 완료.
     * @param amount 실제로 한 양. null 이면 목표량만큼 한 것으로 본다.
     * @param won 승패를 기록하는 퀘스트에서 이겼는지
     */
    suspend fun complete(questId: String, amount: Double? = null, won: Boolean = false)

    /** 완료 취소 (기록은 voided 로 남는다). */
    suspend fun undo(completionId: String)

    /** 완료한 뒤에 더 한 양을 추가한다 (예: 독서 30분 더). */
    suspend fun addAmount(completionId: String, extra: Double)
}

internal class DefaultGameRepository(
    scope: CoroutineScope,
    private val quests: QuestRepository,
    private val completions: CompletionRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
    private val engine: GameEngine = GameEngine(),
    private val rules: GameRules = GameRules.Default,
) : GameRepository {

    override val state: StateFlow<PlayerState?> = combine(
        settings.gameStart,
        quests.quests,
        completions.completions,
        time.todayFlow,
    ) { start, questList, completionList, today ->
        start?.let { engine.compute(EngineInput(it, today, questList, completionList)) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _fx = MutableSharedFlow<Fx>(extraBufferCapacity = 32)
    override val fx: Flow<Fx> = _fx.asSharedFlow()

    /** 방금 한 행동. 다음 상태가 계산되면 이전 상태와 비교해서 연출을 만든다. */
    private sealed interface PendingAction {
        val title: String
        val stats: List<Stat>
    }
    private data class Completed(override val title: String, override val stats: List<Stat>) : PendingAction
    private data class Undone(override val title: String, override val stats: List<Stat> = emptyList()) : PendingAction

    private val pending = AtomicReference<PendingAction?>(null)

    init {
        scope.launch {
            var previous: PlayerState? = null
            state.filterNotNull().collect { current ->
                val before = previous
                previous = current
                val action = pending.getAndSet(null) ?: return@collect
                if (before == null || action is Undone) return@collect
                val gained = (current.totalXp - before.totalXp).toInt()
                if (gained > 0) _fx.emit(Fx.QuestClear(action.title, gained, action.stats))
                FxDiff.between(before, current, rules.perfectDayBonusXp, rules.bossBonusXp)
                    .forEach { _fx.emit(it) }
            }
        }
    }

    override suspend fun complete(questId: String, amount: Double?, won: Boolean) {
        val today = time.today()
        val quest = if (questId == RecoveryQuest.ID) RecoveryQuest.definition(today) else quests.get(questId)
        quest ?: return

        // 이미 끝낸 슬롯이면 무시 (워치와 폰에서 동시에 눌러도 한 번만)
        val todayState = state.value?.today
        val alreadyDone = when {
            questId == RecoveryQuest.ID -> todayState?.recoveryCompletion != null
            else -> todayState?.slots?.any { it.key == quest.slotKey && it.done } == true
        }
        if (alreadyDone) return

        pending.set(Completed(quest.title, quest.weights.keys.toList()))
        completions.save(
            Completion.of(
                quest = quest,
                date = today,
                nowMillis = time.nowMillis(),
                amount = amount ?: quest.target?.amount,
                won = won,
            ),
        )
    }

    override suspend fun undo(completionId: String) {
        val completion = completions.get(completionId) ?: return
        if (completion.voided) return
        pending.set(Undone(completion.questTitle))
        completions.save(completion.copy(voided = true, updatedAt = time.nowMillis()))
    }

    override suspend fun addAmount(completionId: String, extra: Double) {
        if (extra <= 0.0) return
        val completion = completions.get(completionId) ?: return
        if (completion.voided) return
        val current = completion.amount ?: completion.targetAmount ?: 0.0
        pending.set(Completed(completion.questTitle, completion.weights.keys.toList()))
        completions.save(completion.copy(amount = current + extra, updatedAt = time.nowMillis()))
    }
}
