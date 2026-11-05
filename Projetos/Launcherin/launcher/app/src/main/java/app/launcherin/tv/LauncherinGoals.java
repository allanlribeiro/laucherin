package app.launcherin.tv;

import org.json.JSONObject;

/** Persisted baselines: joining a match never announces its old goals or repeats a VAR rollback. */
final class LauncherinGoals {
    static final String KEY = "sports_goal_alerts";
    static final long MAX_GAP = 120000L;
    static boolean observe(JSONObject state, String key, int goals, String phase, long now) throws Exception {
        if (goals < 0 || goals > 99 || (!"in".equals(phase) && !"post".equals(phase))) return false;
        JSONObject before = state.optJSONObject(key);
        int high = before == null ? goals : Math.max(goals, before.optInt("high", goals));
        long stamp = before == null ? 0 : before.optLong("time");
        boolean recent = before != null && now >= stamp && now - stamp <= MAX_GAP;
        boolean goal = recent && "in".equals(before.optString("phase")) && goals > before.optInt("high", goals);
        state.put(key, new JSONObject().put("high", high).put("time", now).put("phase", phase));
        // A bounded history, shared by the two clubs. Never discard the observation being written.
        while (state.length() > 64) {
            String oldest = null; long time = Long.MAX_VALUE;
            java.util.Iterator<String> keys = state.keys();
            while (keys.hasNext()) { String candidate = keys.next(); long t = state.getJSONObject(candidate).optLong("time");
                if (!candidate.equals(key) && t < time) { oldest = candidate; time = t; } }
            if (oldest == null) break; state.remove(oldest);
        }
        return goal;
    }
    static int score(String value) { return value != null && value.matches("[0-9]{1,2}") ? Integer.parseInt(value) : -1; }
}
