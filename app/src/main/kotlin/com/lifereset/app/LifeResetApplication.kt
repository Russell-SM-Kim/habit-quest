package com.lifereset.app

import android.app.Application
import com.lifereset.app.wear.WearStatePublisher
import com.lifereset.core.data.DataContainer
import com.lifereset.core.data.DataContainerOwner
import com.lifereset.core.designsystem.sound.FxSounds
import kotlinx.coroutines.launch

/**
 * 앱 전체의 시작점. 여기서 만든 객체들이 앱이 살아있는 동안 유지된다:
 *  - dataContainer: DB·저장소·동기화·게임 상태
 *  - wearPublisher: 상태가 바뀔 때마다 워치로 보낸다
 *  - sounds: 연출 효과음 (설정의 '효과음' 스위치를 따라간다)
 */
class LifeResetApplication : Application(), DataContainerOwner {

    override val dataContainer: DataContainer by lazy { DataContainer(this) }

    val wearPublisher: WearStatePublisher by lazy { WearStatePublisher(this, dataContainer) }

    val sounds: FxSounds by lazy { FxSounds(this) }

    override fun onCreate() {
        super.onCreate()
        dataContainer.initialize()
        wearPublisher.start()
        dataContainer.appScope.launch {
            dataContainer.settingsRepository.soundEnabled.collect { sounds.enabled = it }
        }
    }
}
