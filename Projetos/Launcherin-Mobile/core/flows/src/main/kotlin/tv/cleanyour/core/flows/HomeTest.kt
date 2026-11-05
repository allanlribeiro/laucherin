package tv.cleanyour.core.flows

import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.ops.ShellRenderer
import tv.cleanyour.core.session.ExecSpec
import tv.cleanyour.core.session.TvSession

enum class HomeTest { LAUNCHER, SCREENSAVER }
enum class HomeTestResult { REQUESTED, FAILED, OFFLINE }

/** Explicit preview only: fixed components, no settings writes, no force-stop or key injection. */
internal object HomeTests {
    fun component(test: HomeTest): Component = when (test) {
        HomeTest.LAUNCHER -> Component(PackageName("app.launcherin.tv"), "app.launcherin.tv.MainActivity")
        HomeTest.SCREENSAVER -> Component(PackageName("com.android.systemui"), "com.android.systemui.Somnambulator")
    }

    suspend fun run(session: TvSession, test: HomeTest): Boolean {
        val rendered = ShellRenderer().render(Op.StartActivity(component(test), wait = true))
        val result = session.exec(rendered.lines.single(), ExecSpec(timeoutMs = 15_000))
        val output = result.stdout + "\n" + result.stderr
        return result.exit == 0 && !result.truncated && "Status: ok" in output &&
            !output.contains("Error", ignoreCase = true) && !output.contains("Exception", ignoreCase = true)
    }
}
