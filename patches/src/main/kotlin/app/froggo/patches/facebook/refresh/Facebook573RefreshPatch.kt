/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * NewsFeedFragmentDataController.refreshForRevisit(...): Z is shared by
 * lifecycle and explicit refresh paths. The lifecycle callers identify
 * themselves with "onResume" and "onAppForeground"; activity-result and
 * fullscreen-close callers use different reasons and must keep the original
 * behavior. NewsFeedFragment.onSetUserVisibleHint and its onPause work also
 * call C2UL.A0A(...), which is the stale-post auto-refresh selector; manual
 * refresh uses a separate path and remains available.
 */
package app.froggo.patches.facebook.refresh

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
private val automaticRefreshForRevisit = Fingerprint(
    returnType = "Z",
    parameters = listOf(
        "Z",
        "Z",
        "Z",
        "I",
        "Ljava/lang/String;",
        "Z",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/2UL;" && method.name == "refreshForRevisit"
    },
)

private val automaticStalePostRefresh = Fingerprint(
    returnType = "V",
    parameters = listOf(
        "LX/2UL;",
        "I",
        "Z",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/2UL;" && method.name == "A0A"
    },
)

@Suppress("unused")
val blockFacebookAutomaticRefresh573Patch = bytecodePatch(
    name = "Block Facebook automatic refresh (573)",
    description = "Experimental: suppresses lifecycle feed refresh while preserving explicit refresh paths; currently unstable on some devices.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL)

    execute {
        automaticStalePostRefresh.method.addInstructions(
            0,
            "return-void",
        )
        automaticRefreshForRevisit.method.addInstructions(
            0,
            """
                const-string v0, "onResume"
                invoke-virtual {v0, p5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-nez v0, :froggo_refresh_skip_lifecycle

                const-string v0, "onAppForeground"
                invoke-virtual {v0, p5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :froggo_refresh_keep_original

                :froggo_refresh_skip_lifecycle
                const/4 v0, 0x0
                return v0

                :froggo_refresh_keep_original
            """.trimIndent(),
        )
    }
}
