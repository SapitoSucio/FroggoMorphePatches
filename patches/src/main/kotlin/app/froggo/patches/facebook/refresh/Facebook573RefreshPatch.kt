/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * NewsFeedFragmentDataController.refreshForRevisit(...): Z is shared by
 * lifecycle and explicit refresh paths. The lifecycle callers identify
 * themselves with "onResume" and "onAppForeground"; activity-result and
 * fullscreen-close callers use different reasons and must keep the original
 * behavior. The synthetic hot-start and foreground runnables are left intact:
 * they maintain feed state and auto-scroll, rather than being the refresh
 * request itself.
 */
package app.froggo.patches.facebook.refresh

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
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

@Suppress("unused")
val blockFacebookAutomaticRefresh573Patch = bytecodePatch(
    name = "Block Facebook automatic refresh (573)",
    description = "Suppresses lifecycle feed refresh while preserving explicit refresh paths.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
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
