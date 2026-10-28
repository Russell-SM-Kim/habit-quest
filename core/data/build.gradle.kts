// 저장(Room)과 동기화(서버). 화면 모듈은 여기 있는 Repository 인터페이스만 본다.
plugins {
    alias(libs.plugins.lifereset.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.lifereset.core.data"
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
