package tv.cleanyour.core.planner

import tv.cleanyour.core.model.*
import tv.cleanyour.core.spec.LauncherConfigDoc
import tv.cleanyour.core.planner.PlannerFixtures.installedLauncher
import tv.cleanyour.core.planner.PlannerFixtures.snapshot
import tv.cleanyour.core.planner.PlannerFixtures.plan
import tv.cleanyour.core.planner.PlannerFixtures.refused
import kotlin.test.*

class WidgetFramePlanTest {
    private val photo=BlobRef(Sha256("a".repeat(64)),1024,"/cache/photo.jpg")
    private val config=BlobRef(Sha256("b".repeat(64)),100,"/cache/config.json")
    @Test fun imagesAreVerifiedAndSentBeforeTheConfigurationReferencesThem() {
        val snapshot=snapshot(extra=mapOf(installedLauncher(versionCode=18))).let { it.copy(facts=it.facts.copy(sdk=34)) }
        val ops=plan(PlanRequest.ApplyTheme(LauncherConfigDoc(),config,null,listOf(photo)),snapshot,planner=PlannerFixtures.planner(OverlayTransport())).steps.map { it.op }
        val image=ops.filterIsInstance<Op.PushAsset>().single { it.src==photo }
        assertEquals("frame-${photo.sha256.hex.take(40)}.jpg",image.dest.segments.single())
        assertEquals(FileRoute.THEME_PROVIDER,image.route)
        assertTrue(ops.indexOf(image)<ops.indexOfFirst { it is Op.PushAsset && it.src==config })
    }
    @Test fun olderLaunchersAndUnboundedCollectionsAreRefused() {
        assertTrue(refused(PlanRequest.ApplyTheme(LauncherConfigDoc(),config,null,listOf(photo)),snapshot(extra=mapOf(installedLauncher(versionCode=17)))).any { "update" in it })
        assertTrue(refused(PlanRequest.ApplyTheme(LauncherConfigDoc(),config,null,List(11){photo}),snapshot(extra=mapOf(installedLauncher(versionCode=18)))).any { "photo" in it })
    }
}
