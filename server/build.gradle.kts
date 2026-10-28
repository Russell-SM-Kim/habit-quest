// Windows PC 에서 24시간 도는 동기화 서버.
// 실행:   gradlew -PserverOnly=true :server:run
// 배포용: gradlew -PserverOnly=true :server:buildFatJar  -> server/build/libs/life-reset-server.jar
plugins {
    alias(libs.plugins.lifereset.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("com.lifereset.server.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("life-reset-server.jar")
    }
}

dependencies {
    implementation(project(":core:protocol"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.logback.classic)
    implementation(libs.google.api.client) // Google 로그인 토큰 검증

    testImplementation(libs.ktor.server.test.host)
}
