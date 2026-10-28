package com.lifereset.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
 *
 * 앱이 화면에 나타날 때마다(처음 켤 때, 다른 앱에서 돌아올 때) 바로 동기화한다.
 * 로그인 전이면 SyncManager 가 아무것도 하지 않는다.
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
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = dataContainer.syncScheduler.syncNow()
        })
    }
}
