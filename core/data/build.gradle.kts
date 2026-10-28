import java.util.Properties

// 저장(Room)과 동기화(서버). 화면 모듈은 여기 있는 Repository 인터페이스만 본다.
plugins {
    alias(libs.plugins.lifereset.android.library)
    alias(libs.plugins.ksp)
}

/*
 * 기본 서버 주소: 앱을 처음 켠 사람이 주소를 입력하지 않아도 되게 앱에 넣어 둔다.
 * 공개 저장소에 내 서버 주소가 올라가지 않도록 git 에 안 올라가는 local.properties 에서 읽는다.
 *   habitquest.serverUrl=https://내PC.tailnet이름.ts.net
 * 없으면 빈 값 → 앱의 시작 화면에서 직접 입력.
 */
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val defaultServerUrl: String = (
    localProperties.getProperty("habitquest.serverUrl")
        ?: providers.gradleProperty("habitquest.serverUrl").orNull
        ?: ""
    ).trim().trimEnd('/')

android {
    namespace = "com.lifereset.core.data"

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"$defaultServerUrl\"")
    }
}

ksp {
    // DB 구조 변경 이력을 남겨서 나중에 마이그레이션을 쓸 수 있게 한다
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:game"))
    api(project(":core:protocol"))
    api(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)

    // Google 로그인 (계정 선택 창 → ID 토큰)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
