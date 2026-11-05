package tv.cleanyour.core.flows

import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.Op
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.ShellLine
import tv.cleanyour.core.ops.HomePreviewLines
import tv.cleanyour.core.ops.ShellRenderer
import tv.cleanyour.core.session.ExecSpec
import tv.cleanyour.core.session.TvSession

enum class HomeTest { LAUNCHER, SCREENSAVER }
enum class HomeTestResult { REQUESTED, FAILED, OFFLINE }

/** Fixed launcher component or public Dream settings action; no settings writes or key injection. */
internal object HomeTests {
    fun line(test: HomeTest): ShellLine = when(test) {
        HomeTest.LAUNCHER -> ShellRenderer().render(Op.StartActivity(
            Component(PackageName("app.launcherin.tv"), "app.launcherin.tv.MainActivity"), wait = true)).lines.single()
        HomeTest.SCREENSAVER -> HomePreviewLines.dreamSettings()
    }

    suspend fun run(session: TvSession, test: HomeTest): Boolean {
        val result = session.exec(line(test), ExecSpec(timeoutMs = 15_000))
        val output = result.stdout + "\n" + result.stderr
        return result.exit == 0 && !result.truncated && "Status: ok" in output &&
            !output.contains("Error", ignoreCase = true) && !output.contains("Exception", ignoreCase = true)
    }
}
