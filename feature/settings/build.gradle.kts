plugins {
    alias(libs.plugins.lifereset.android.feature)
}

android {
    namespace = "com.lifereset.feature.settings"
}

dependencies {
    // 배경 이미지 고르기 (사진 선택 창)
    implementation(libs.androidx.activity.compose)
}
