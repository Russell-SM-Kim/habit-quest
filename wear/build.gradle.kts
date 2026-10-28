// 갤럭시 워치(Wear OS) 앱. 폰 앱의 '화면 + 리모컨' 역할만 한다.
// 계산·저장은 전부 폰이 하고, 워치는 폰이 보내준 요약(WearSnapshot)을 보여주고 완료 요청만 보낸다.
plugins {
    alias(libs.plugins.lifereset.android.application)
}

android {
    namespace = "com.lifereset.wear"

    defaultConfig {
        // 폰 앱과 반드시 같아야 데이터 레이어로 통신된다 (서명 키도 같아야 함)
        applicationId = "com.lifereset.app"
        minSdk = 30
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // 워치는 core:game(모델)과 core:protocol(주고받는 형식)만 본다. 폰 UI 모듈은 쓰지 않는다.
    implementation(project(":core:game"))
    implementation(project(":core:protocol"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
}
