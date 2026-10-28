import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.lifereset.buildlogic.AppSdk
import com.lifereset.buildlogic.configureAndroidCompose
import com.lifereset.buildlogic.configureKotlinAndroid
import com.lifereset.buildlogic.configureKotlinJvm
import com.lifereset.buildlogic.lib
import com.lifereset.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType

/**
 * 폰/워치 앱 모듈. Compose 까지 켠다.
 * minSdk 는 모듈에서 덮어쓸 수 있다 (워치는 30).
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureAndroidCompose(this)
                defaultConfig.targetSdk = AppSdk.TARGET
            }
        }
    }
}

/** UI 없는 안드로이드 라이브러리 (예: core:data). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
            }
        }
    }
}

/** 안드로이드 라이브러리에 Compose 를 켠다. lifereset.android.library 와 함께 쓴다. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            @Suppress("UNCHECKED_CAST")
            val android = extensions.getByName("android") as CommonExtension<*, *, *, *, *, *>
            configureAndroidCompose(android)
        }
    }
}

/**
 * 화면 모듈(feature:*) 공통 설정.
 * 화면은 core:data 의 저장소와 core:designsystem 의 컴포넌트만 보고, 다른 화면 모듈은 모른다.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("lifereset.android.library")
            pluginManager.apply("lifereset.android.compose")
            dependencies {
                add("implementation", project(":core:game"))
                add("implementation", project(":core:data"))
                add("implementation", project(":core:designsystem"))
                add("implementation", libs.lib("androidx-compose-material3"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("kotlinx-coroutines-android"))
            }
        }
    }
}

/** 안드로이드에 의존하지 않는 순수 Kotlin 모듈 (core:game, core:protocol). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            dependencies {
                add("testImplementation", kotlin("test"))
            }
            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }
}
