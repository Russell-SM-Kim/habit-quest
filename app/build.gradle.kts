// 폰 앱. 각 모듈을 조립하고(AppContainer), 화면을 연결하고(Navigation), 워치와 통신한다.
plugins {
    alias(libs.plugins.lifereset.android.application)
}

android {
    namespace = "com.lifereset.app"

    defaultConfig {
        // 워치 앱(wear)과 반드시 같아야 데이터 레이어로 통신된다
        applicationId = "com.lifereset.app"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 혼자 쓰는 앱이라 디버그 키로 서명한다. 폰·워치를 같은 PC 에서 빌드하면 키가 같아진다.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":core:game"))
    implementation(project(":core:protocol"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:today"))
    implementation(project(":feature:status"))
    implementation(project(":feature:record"))
    implementation(project(":feature:quests"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
}
