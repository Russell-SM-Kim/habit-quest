// 폰 <-> 서버, 폰 <-> 워치가 주고받는 데이터 형식. 형식을 바꾸면 양쪽이 함께 바뀐다.
plugins {
    alias(libs.plugins.lifereset.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:game"))
    api(libs.kotlinx.serialization.json)
}
