package app.launcherin.tv;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherinPlaybackSessionTest {
    @Test public void anotherEpisodeWithTheSameTitleIsNotTheClickedItem() throws Exception {
        JSONObject saved = new JSONObject().put("title", "Uma série").put("media_id", "episode-2");
        assertFalse(LauncherinPlayback.sessionMatches(saved, "Uma série", "episode-3"));
    }

    @Test public void anExactMediaIdSurvivesDisplayTitleChanges() throws Exception {
        JSONObject saved = new JSONObject().put("title", "Título antigo").put("media_id", "episode-2");
        assertTrue(LauncherinPlayback.sessionMatches(saved, "Título traduzido", "episode-2"));
    }

    @Test public void missingSessionIdCannotOverrideAKnownDifferentEpisode() throws Exception {
        JSONObject saved = new JSONObject().put("title", "Uma série").put("media_id", "episode-2");
        assertFalse(LauncherinPlayback.sessionMatches(saved, "Uma série", null));
        assertFalse(LauncherinPlayback.sessionMatches(saved, "Uma série", ""));
    }

    @Test public void oldHistoryRequiresMatchingTitlesInsteadOfJustTheSameApp() throws Exception {
        JSONObject saved = new JSONObject().put("title", " Filme A ");
        assertTrue(LauncherinPlayback.sessionMatches(saved, "Filme A", null));
        assertFalse(LauncherinPlayback.sessionMatches(saved, "Filme B", null));
    }

    @Test public void missingTitlesAreNotEvidenceOfTheSameContent() throws Exception {
        assertFalse(LauncherinPlayback.sessionMatches(new JSONObject(), "", null));
        assertFalse(LauncherinPlayback.sessionMatches(new JSONObject().put("title", "Filme"), null, null));
    }
}
