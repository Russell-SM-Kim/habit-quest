pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "life-reset"

// 순수 Kotlin: 게임 규칙과 통신 형식 (폰, 워치, 서버가 공유)
include(":core:game")
include(":core:protocol")

// 서버
include(":server")

// Android SDK가 없는 PC에서 서버만 돌릴 때: gradlew -PserverOnly=true :server:run
val serverOnly = providers.gradleProperty("serverOnly").orNull == "true"
if (!serverOnly) {
    // 안드로이드 공용
    include(":core:data")
    include(":core:designsystem")

    // 화면 (기능 하나 = 모듈 하나)
    include(":feature:onboarding")
    include(":feature:today")
    include(":feature:status")
    include(":feature:record")
    include(":feature:quests")
    include(":feature:settings")

    // 앱
    include(":app")
    include(":wear")
}
