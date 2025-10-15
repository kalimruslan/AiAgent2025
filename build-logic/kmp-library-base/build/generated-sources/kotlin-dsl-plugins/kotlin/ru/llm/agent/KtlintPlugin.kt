package ru.llm.agent


/**
 * Precompiled [ktlint.gradle.kts][ru.llm.agent.Ktlint_gradle] script plugin.
 *
 * @see ru.llm.agent.Ktlint_gradle
 */
public
class KtlintPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("ru.llm.agent.Ktlint_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
