// 유리창(SAO 풍) 테마: 색, 글꼴, 아이콘, 배경, 효과음, 공용 컴포넌트, 연출(레벨업 등).
// 디자인을 바꿀 때는 theme/ 폴더(색·글꼴)와 res/ 폴더(이미지·폰트)만 고치면 된다.
plugins {
    alias(libs.plugins.lifereset.android.library)
    alias(libs.plugins.lifereset.android.compose)
}

android {
    namespace = "com.lifereset.core.designsystem"
}

dependencies {
    api(project(":core:game"))
    api(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
}
