package tv.cleanyour.core.trust

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.cleanyour.core.model.BlobRef
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.Sha256
import tv.cleanyour.core.store.BlobStore
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

class LauncherPinTest {
    private val apk = File(Repo.bundled, PinnedLauncher.ASSET_NAME)

    @Test fun bundledApkMatchesThePin() {
        assertEquals(emptyList<String>(), LauncherPinCheck.problems(apk))
    }

    @Test fun aModifiedApkFailsTheCheck() {
        val tmp = File.createTempFile("launcher", ".apk").apply { deleteOnExit() }
        val bytes = apk.readBytes().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 1).toByte() }
        tmp.writeBytes(bytes)
        val problems = LauncherPinCheck.problems(tmp)
        assertTrue(problems.toString(), problems.any { it.startsWith("sha256") })
        assertTrue(LauncherPinCheck.problems(File("/nonexistent.apk")).isNotEmpty())
    }

    @Test fun pinIsTheLauncherinPackageWithItsActualSourceBase() {
        assertEquals("app.launcherin.tv", PinnedLauncher.PACKAGE)
        assertEquals("e62aa10a033f715a8ac7116efcf91e01b145adbd", PinnedLauncher.UPSTREAM_COMMIT)
        // The separate submodule is still the pinned upstream profile dataset, not the fork APK's source.
    }

    @Test fun bundledLauncherPinServesThePinnedBlob() {
        val bytes = apk.readBytes()
        val artifact = BundledLauncherPin(OneBlob(PinnedLauncher.ASSET_NAME, bytes)).bundled()
        assertEquals(PackageName("app.launcherin.tv"), artifact.pkg)
        assertEquals(26L, artifact.versionCode)
        assertEquals("0.1.0-preview18", artifact.versionName)
        assertEquals(Sha256(PinnedLauncher.SHA256), artifact.sha256)
        assertEquals(Sha256(PinnedLauncher.SIGNER_SHA256), artifact.signer)
        assertEquals(artifact.sha256, artifact.blob.sha256)
    }

    @Test fun bundledLauncherPinRefusesASwappedAsset() {
        val other = "not the launcher".toByteArray()
        assertThrows(IllegalStateException::class.java) { BundledLauncherPin(OneBlob(PinnedLauncher.ASSET_NAME, other)).bundled() }
    }

    private class OneBlob(private val name: String, private val bytes: ByteArray) : BlobStore {
        private val ref = BlobRef(Sha256(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }), bytes.size.toLong(), "mem:$name")
        override suspend fun put(sha256: Sha256, source: InputStream, expectedSize: Long): BlobRef = error("read only")
        override fun get(sha256: Sha256): BlobRef? = ref.takeIf { it.sha256 == sha256 }
        override suspend fun delete(sha256: Sha256) = Unit
        override fun bundled(name: String): BlobRef = if (name == this.name) ref else error("no $name")
    }
}
