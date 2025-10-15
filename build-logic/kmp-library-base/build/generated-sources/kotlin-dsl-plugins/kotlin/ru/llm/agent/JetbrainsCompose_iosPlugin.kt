package ru.llm.agent


/**
 * Precompiled [jetbrains-compose.ios.gradle.kts][ru.llm.agent.Jetbrains_compose_ios_gradle] script plugin.
 *
 * @see ru.llm.agent.Jetbrains_compose_ios_gradle
 */
public
class JetbrainsCompose_iosPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("ru.llm.agent.Jetbrains_compose_ios_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
