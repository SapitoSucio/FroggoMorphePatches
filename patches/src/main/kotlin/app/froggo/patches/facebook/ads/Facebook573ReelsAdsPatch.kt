package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private fun reelsExactMethod(
    classDescriptor: String,
    methodName: String,
    parameters: List<String> = emptyList(),
) = Fingerprint(
    parameters = parameters,
    custom = { method, classDef ->
        classDef.type == classDescriptor && method.name == methodName
    },
)

/*
 * Facebook 573.0.0.37.74 / 473623755 - Reels/video ads only.
 *
 * Existing proven seams cover:
 * - Reels/video banner and video-ad fetch: X.5Vs.A03(...)
 * - banner/card callback: X.62B.onSuccess(Object)
 * - video-ad callback: X.9mO.onSuccess(Object)
 * - commercial-break fetch/state machine: X.Qsb.A05 / X.Qsw.onSuccess
 *
 * Swipe-in sponsored Reels use a different path. JADX shows X.51K.A0G(...)
 * converting each upstream X.53B item into a visible Reels item and appending
 * it to the destination List. The stock method resolves:
 *
 *   53B.A00() -> 1sX.A1K() -> 7KX.A01() -> 9D5.AGg() -> BsO.BN1()
 *
 * and compares BN1() with X.6Xn.A02, which is the enum constant AD. Returning
 * before the builder runs drops only the sponsored Reel item while leaving UGC,
 * PARADE and MIDCARD items on their stock path.
 */
private val videoAdBreakFetch = reelsExactMethod(
    "LX/Qsb;",
    "A05",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/41Q;",
        "LX/4ta;",
        "I",
        "Z",
        "Z",
    ),
)

private val videoAdBreakSuccess = reelsExactMethod(
    "LX/Qsw;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val reelsVideoAdFetch = reelsExactMethod(
    "LX/5Vs;",
    "A03",
    listOf(
        "LX/5Vw;",
        "LX/41Q;",
        "LX/caj;",
        "LX/5I6;",
        "Ljava/lang/Boolean;",
        "Ljava/lang/Integer;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "I",
        "I",
        "J",
        "Z",
        "Z",
        "Z",
    ),
)

private val reelsBannerAdSuccess = reelsExactMethod(
    "LX/62B;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val reelsVideoAdSuccess = reelsExactMethod(
    "LX/9mO;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val reelsSwipeItemBuilder = reelsExactMethod(
    "LX/51K;",
    "A0G",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/1sX;",
        "LX/COt;",
        "LX/7QH;",
        "LX/53B;",
        "Ljava/util/List;",
    ),
)

@Suppress("unused")
val blockFacebookReelsAds573Patch = bytecodePatch(
    name = "Block Facebook Reels ads (573)",
    description = "Blocks sponsored Reels in the swipe feed plus Reels/video banners, video ads, and commercial breaks.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        reelsSwipeItemBuilder.method.addInstructions(
            0,
            """
                move-object/from16 v0, p5
                invoke-virtual {v0}, LX/53B;->A00()LX/1sX;
                move-result-object v0
                if-eqz v0, :froggo_reelsads573_keep_item
                invoke-virtual {v0}, LX/1sX;->A1K()LX/7KX;
                move-result-object v0
                if-eqz v0, :froggo_reelsads573_keep_item
                invoke-virtual {v0}, LX/7KX;->A01()LX/9D5;
                move-result-object v0
                if-eqz v0, :froggo_reelsads573_keep_item
                invoke-interface {v0}, LX/9D5;->AGg()LX/BsO;
                move-result-object v0
                if-eqz v0, :froggo_reelsads573_keep_item
                invoke-interface {v0}, LX/BsO;->BN1()LX/6Xn;
                move-result-object v0
                sget-object v1, LX/6Xn;->A02:LX/6Xn;
                if-ne v0, v1, :froggo_reelsads573_keep_item
                return-void

                :froggo_reelsads573_keep_item
            """.trimIndent(),
        )

        videoAdBreakFetch.method.addInstructions(
            0,
            "return-void",
        )
        videoAdBreakSuccess.method.addInstructions(
            0,
            "return-void",
        )
        reelsVideoAdFetch.method.addInstructions(
            0,
            "return-void",
        )
        reelsBannerAdSuccess.method.addInstructions(
            0,
            "return-void",
        )
        reelsVideoAdSuccess.method.addInstructions(
            0,
            "return-void",
        )
    }
}
