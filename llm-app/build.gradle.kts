import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.frameio.kmplib)
    alias(libs.plugins.frameio.compose)
    alias(libs.plugins.manifestGuard)
    alias(libs.plugins.jetbrains.compose.hotreload)
}

// Загружаем local.properties
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.chat)
            implementation(projects.core.utils)
            implementation(projects.llmData)
            implementation(projects.llmDomain)
            implementation(compose.components.resources)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.material)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }

    compilerOptions {
        // Code running from IDEA/Android Studio
        if (System.getProperty("idea.active") == "true") {
            // Turn on debug mode
            freeCompilerArgs = listOf("-Xdebug")
        }
    }
}

compose.desktop {
    application {
        mainClass = "ru.llm.agent.AgentDesktopKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ru.llm.agent"
            packageVersion = "1.0.0"
        }

        // Прокидываем переменные как JVM properties
        jvmArgs += localProperties.map { (key, value) ->
            "-D$key=$value"
        }
    }
}


android {
    namespace = "ru.llm.agent"

    defaultConfig {
        applicationId = "ru.llm.agent"
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")

            // Disables PNG crunching for the "release" build type.
            isCrunchPngs = false
        }
    }

    androidResources {
        generateLocaleConfig = false
        localeFilters += setOf("ru", "en")
    }

    lint {
        checkReleaseBuilds = false
        checkDependencies = true
    }

    buildFeatures {
        buildConfig = true
    }
}

androidComponents.beforeVariants { variant ->
    manifestGuard {
        guardVariant(variant.name) {
            enabled = true
            compareOnAssemble = false

            val baseDir = layout.projectDirectory.dir("manifest/${variant.name}")
            referenceFile = baseDir.file("manifest-original.xml")
            htmlDiffFile = baseDir.file("manifest-diff.html")

            ignore {
                ignoreAppVersionChanges = true
            }
        }
    }
}
