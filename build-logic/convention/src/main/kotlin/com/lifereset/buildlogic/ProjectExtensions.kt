package com.lifereset.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** 버전 카탈로그(gradle/libs.versions.toml)를 컨벤션 플러그인 안에서 쓰기 위한 접근자. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("libs.versions.toml 에 '$alias' 가 없습니다") }

/** 모든 안드로이드 모듈이 공유하는 SDK 버전. 바꿀 땐 여기 한 곳만 고친다. */
object AppSdk {
    const val COMPILE = 35
    const val TARGET = 35
    const val MIN_PHONE = 26
    const val MIN_WEAR = 30
}
