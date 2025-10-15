import com.google.devtools.ksp.gradle.KspAATask
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.frameio.kmplib)
    alias(libs.plugins.google.ksp)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict

    sourceSets {
        commonMain {
            dependencies {
            }
        }
    }
}

android {
    namespace = "ru.llm.agent.business"
}

afterEvaluate {
    tasks.withType<KspAATask>().configureEach {
        if (name != "kspCommonMainKotlinMetadata") {
            dependsOn(tasks.named("kspCommonMainKotlinMetadata"))
        }
    }
}
