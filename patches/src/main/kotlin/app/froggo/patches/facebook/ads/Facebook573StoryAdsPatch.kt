package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/*
 * Pre-release only: this file lives on the dev branch and is intentionally
 * absent from main/stable.
 *
 * Facebook 573.0.0.37.74 / 473623755:
 * X.AmP.A00(FbUserSession, StoryBucketLaunchConfig, X.BsJ): Z is the native
 * Story Ads eligibility gate. Aky.A0C only creates/registers the Story ad
 * provider when this returns true. AmB.D3X also treats false as the normal
 * is_ads_fetching_enabled=false state.
 *
 * This experiment is default-off until the Story black-screen regression is
 * understood from runtime logs.
 */
private val storyAdsEligibility = Fingerprint(
    returnType = "Z",
    parameters = listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "Lcom/facebook/stories/model/StoryBucketLaunchConfig;",
        "LX/BsJ;",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/AmP;" && method.name == "A00"
    },
)

@Suppress("unused")
val blockFacebookStoryAds573Patch = bytecodePatch(
    name = "Block Facebook Story ads (573)",
    description = "Pre-release experiment: disables Facebook's native Story Ads eligibility gate.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        storyAdsEligibility.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
    }
}
