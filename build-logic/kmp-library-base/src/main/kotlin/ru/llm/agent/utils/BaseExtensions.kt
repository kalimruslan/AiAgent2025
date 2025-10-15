@file:Suppress("ktlint")

package ru.llm.agent.utils

import com.android.build.api.dsl.AndroidResources
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DefaultConfig
import com.android.build.api.dsl.Installation
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.dsl.TestExtension
import com.android.build.gradle.internal.dsl.DynamicFeatureExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

private typealias AndroidExtensions = CommonExtension<
        out BuildFeatures,
        out BuildType,
        out DefaultConfig,
        out ProductFlavor,
        out AndroidResources,
        out Installation,
        >

private val Project.androidExtension: AndroidExtensions
    get() {
        return extensions.findByType(ApplicationExtension::class)
            ?: extensions.findByType(LibraryExtension::class)
            ?: extensions.findByType(DynamicFeatureExtension::class)
            ?: extensions.findByType(TestExtension::class)
            ?: error(
                "\"Project.androidExtension\" value may be called only "
                        + "from android application"
                        + " or android library gradle script",
            )
    }

internal fun Project.androidConfig(
    block: AndroidExtensions.() -> Unit
): Unit = block(androidExtension)

internal fun Project.kotlinJvmCompilerOptions(block: KotlinJvmCompilerOptions.() -> Unit) {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions(block)
    }
}

internal val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

private val Project.kmpExtension: KotlinMultiplatformExtension
    get() {
        return extensions.findByType(KotlinMultiplatformExtension::class) ?: error(
            "\"Project.kmpExtension\" value may be called only " + "from kotlin multiplatform gradle script",
        )
    }

internal inline fun Project.kmpConfig(
    block: KotlinMultiplatformExtension.() -> Unit
) = kmpExtension.block()

val Project.composeExt: ComposeExtension
    get() = extensions.findByType(ComposeExtension::class.java)
        ?: error("Compose plugin is not applied")

@Suppress("MagicNumber")
internal fun LibrariesForLibs.javaVersion(target: ProjectTargets.JvmTarget): JavaVersion {
    val jdkVersion = jvmVersion(target)
    require(jdkVersion >= 10)
    return JavaVersion.toVersion(jdkVersion)
}

private fun LibrariesForLibs.jvmVersion(target: ProjectTargets.JvmTarget): Int {
    return when (target) {
        ProjectTargets.Android -> versions.jdkAndroid.get().toInt()
        ProjectTargets.Desktop -> versions.jdkDesktop.get().toInt()
        else -> 0
    }
}

@Suppress("MagicNumber")
internal fun LibrariesForLibs.jvmTarget(target: ProjectTargets.JvmTarget): JvmTarget {
    val jdkVersion = jvmVersion(target)
    require(jdkVersion >= 10)
    return JvmTarget.valueOf("JVM_$jdkVersion")
}

internal val Project.detektExtension: DetektExtension
    get() = checkNotNull(extensions.findByType(DetektExtension::class))

internal fun Project.detektConfig(block: DetektExtension.() -> Unit) = block(detektExtension)
