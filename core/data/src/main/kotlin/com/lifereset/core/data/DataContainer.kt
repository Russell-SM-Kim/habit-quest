package com.lifereset.core.data

import android.content.Context
import com.lifereset.core.data.db.LifeResetDatabase
import com.lifereset.core.data.repository.CompletionRepository
import com.lifereset.core.data.repository.DefaultGameRepository
import com.lifereset.core.data.repository.GameRepository
import com.lifereset.core.data.repository.LocalSettingsRepository
import com.lifereset.core.data.repository.OfflineCompletionRepository
import com.lifereset.core.data.repository.OfflineQuestRepository
import com.lifereset.core.data.repository.QuestRepository
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.data.sync.ServerApi
import com.lifereset.core.data.sync.SyncManager
import com.lifereset.core.data.sync.WorkManagerSyncScheduler
import com.lifereset.core.data.time.SystemTimeProvider
import com.lifereset.core.data.time.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Application 이 구현한다. WorkManager·워치 서비스처럼 화면 밖에서 저장소가 필요할 때 쓴다. */
interface DataContainerOwner {
    val dataContainer: DataContainer
}

/**
 * 데이터 계층의 조립 지점 (수동 DI).
 * 무엇이 무엇에 연결되는지 이 파일 하나만 보면 알 수 있다.
 * 앱 전체에서 하나만 만든다 (Application 에서).
 */
class DataContainer(context: Context) {
    private val appContext = context.applicationContext

    /** 앱이 살아있는 동안 유지되는 스코프. 상태 계산과 연출 감지가 여기서 돈다. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val time: TimeProvider = SystemTimeProvider()

    private val database: LifeResetDatabase = LifeResetDatabase.create(appContext)

    val syncScheduler = WorkManagerSyncScheduler(appContext)

    val settingsRepository: SettingsRepository =
        LocalSettingsRepository(database.settingDao(), time, syncScheduler)

    val questRepository: QuestRepository =
        OfflineQuestRepository(database.questDao(), time, syncScheduler)

    val completionRepository: CompletionRepository =
        OfflineCompletionRepository(database.completionDao(), syncScheduler)

    val gameRepository: GameRepository = DefaultGameRepository(
        scope = appScope,
        quests = questRepository,
        completions = completionRepository,
        settings = settingsRepository,
        time = time,
    )

    val syncManager = SyncManager(database, ServerApi(), settingsRepository, time)

    /** 앱 시작 시 1회: 첫 실행이면 시작일과 기본 퀘스트를 만들고, 주기 동기화를 건다. */
    fun initialize() {
        appScope.launch {
            val start = settingsRepository.gameStart() ?: time.today().also { settingsRepository.setGameStart(it) }
            questRepository.seedIfEmpty(start)
        }
        syncScheduler.schedulePeriodic()
    }
}
