/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * Validated against the target APK with JADX/MCP and the DEX string table:
 * - X.AmP.A00(FbUserSession, StoryBucketLaunchConfig, X.BsJ): Z
 *   (native Story Ads eligibility gate; false prevents provider creation)
 * - MainFeedCSRDataLoaderImpl$handlerTailLoadEvent$2 -> run(): V
 * - MainFeedCSRDataLoaderImpl$maybeDoAsyncAdsTailLoad$1 -> run(): V
 * - MainFeedCSRDataLoaderImpl.maybeDoAsyncAdsTailLoad -> A08(X.1wV): V
 * - FeedCSRAdChannelControllerImpl converter -> X.bZU.A00(...): X.3JJ
 * - AdBreakFetchHelper -> A05(...): V
 * - AdBreakStateMachine callback -> onSuccess(Object): V
 * - X.5Vs.A03(...): V (Reels/video banner and video ad fetch)
 * - X.62B.onSuccess(Object): V (banner/card ad callback)
 * - X.9mO.onSuccess(Object): V (video ad callback)
 * - X.3JX.A0F(...): X.6Ke (FeedAsyncAdsController ASYNC_ADS edge conversion;
 *   returns an empty C6Ke because DbP consumes both fields)
 * - X.1vv.addNewEdgeToCollection(...): Z (final feed UI insertion filter for
 *   SPONSORED/PROMOTION edges)
 * - GraphQLFBMultiAdsFeedUnit.A00(): X.41Q
 * - GraphQLPartialStory.getSponsoredData(): X.41Q
 */

/*
 * Findings and maintenance guide for future Facebook versions
 *
 * Facebook 573 does not have one ad pipeline. The working map is:
 *
 * 1. Feed CSR: C23I.A0F -> bZU.A00 -> GraphQLFeedUnitEdge. The converter
 *    produces feed units from the CSR response.
 * 2. Feed async ads: 3JX.A0F (FeedAsyncAdsController) can build ASYNC_ADS
 *    edges after the normal feed response. DbP consumes its C6Ke result.
 * 3. Final feed insertion: 1vv.addNewEdgeToCollection(...) is the last
 *    source-independent seam before the feed collection reaches the UI.
 *    In this APK GraphQLFeedStoryCategory.A0K is SPONSORED and A0I is
 *    PROMOTION; both are rejected there as a defensive final filter.
 * 4. Story Ads: do not mutate StoryViewerBucketDataController, AuI/WXO,
 *    navigation, bucket lists, pagination callbacks, or C9XO state. Facebook
 *    already has a native eligibility gate at AmP.A00(...). Returning false
 *    follows the app's own "Story Ads disabled" path, so Aky.A0C does not
 *    create/register the Story ad provider at all. This avoids placeholders,
 *    provider completion contracts, and bucket-index desynchronization.
 * 5. Reels/video: 5Vs.A03 starts banner/video ad work and 62B/9mO consume
 *    banner/card and video callbacks. Qsb.A05 and Qsw.onSuccess handle the
 *    separate commercial-break / AdBreak state machine, including
 *    NON_INTERRUPTIVE_AD.
 * 6. Model fallbacks: GraphQLFBMultiAdsFeedUnit.A00 and
 *    GraphQLPartialStory.getSponsoredData expose sponsored data to later
 *    renderers, so they are kept as narrow null-return guards.
 *
 * To port this patch to a new APK:
 *
 * - Extract the DEX files and run the local string-table scanner first. Use
 *   narrow terms such as ASYNC_ADS, AdBreakServerAPI, NON_INTERRUPTIVE_AD,
 *   GraphQLFeedUnitEdge, banner ads fetch, video ad fetch, and sponsored.
 *   Example:
 *   python C:\Users\Administrator\Documents\Codex\2026-08-25\q\work\dex_string_xrefs.py <dex-dir> ASYNC_ADS NON_INTERRUPTIVE_AD
 * - Use each scanner hit to identify the raw LX/... descriptor and exact
 *   method. Then inspect only those methods in JADX MCP, sequentially and
 *   with low result limits. Prefer a method lookup or smali view, followed
 *   by at most a small xref query; avoid global keyword searches.
 * - Trace each route from response/callback -> converter -> feed/model list
 *   -> collection insertion -> renderer. Keep feed, Story, commercial-break,
 *   and non-interruptive video routes separate; they only partially overlap.
 * - Patch the latest ad-specific seam that still leaves ordinary content
 *   intact. If a new feed pipeline bypasses the loaders, look for its final
 *   GraphQLFeedUnitEdge/list insertion rather than only blocking networking.
 * - Reconfirm every descriptor and constructor against the exact target APK,
 *   compile and apply to that APK, then test feed, Story, full-screen video,
 *   and the card below video independently.
 *
 * Register aliases are physical registers: p0 can be v16+ in a large method.
 * Use low v registers for injected const/4, return-object, invoke-*, and
 * field instructions.
 */
package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

private fun redexRunnable(originalName: String) = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        method.name == "run" && classDef.fields.any { field ->
            field.name == "__redex_internal_original_name" &&
                (field.initialValue as? StringEncodedValue)?.value == originalName
        }
    },
)

private fun exactMethod(
    classDescriptor: String,
    methodName: String,
    parameters: List<String> = emptyList(),
) = Fingerprint(
    parameters = parameters,
    custom = { method, classDef ->
        classDef.type == classDescriptor && method.name == methodName
    },
)

private val mainFeedTailLoad = redexRunnable(
    "MainFeedCSRDataLoaderImpl\$handlerTailLoadEvent\$2",
)

private val mainFeedAsyncAdsTailLoadRunnable = redexRunnable(
    "MainFeedCSRDataLoaderImpl\$maybeDoAsyncAdsTailLoad\$1",
)

private val mainFeedAsyncAdsTailLoad = exactMethod(
    "LX/1wV;",
    "A08",
    listOf("LX/1wV;"),
)

private val feedAdsResponseConverter = exactMethod(
    "LX/bZU;",
    "A00",
    listOf(
        "Lcom/facebook/api/feed/model/FetchFeedParams;",
        "LX/3pN;",
        "LX/41R;",
    ),
)

private val storyAdsEligibility = exactMethod(
    "LX/AmP;",
    "A00",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "Lcom/facebook/stories/model/StoryBucketLaunchConfig;",
        "LX/BsJ;",
    ),
)

private val videoAdBreakFetch = exactMethod(
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

private val videoAdBreakSuccess = exactMethod(
    "LX/Qsw;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val reelsVideoAdFetch = exactMethod(
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

private val reelsBannerAdSuccess = exactMethod(
    "LX/62B;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val reelsVideoAdSuccess = exactMethod(
    "LX/9mO;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val asyncFeedAdsController = exactMethod(
    "LX/3JX;",
    "A0F",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/3pN;",
        "Lcom/facebook/graphql/executor/GraphQLResult;",
    ),
)

private val feedEdgeInsertion = exactMethod(
    "LX/1vv;",
    "addNewEdgeToCollection",
    listOf(
        "Lcom/google/common/collect/ImmutableList\$Builder;",
        "Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;",
        "LX/1cP;",
    ),
)

private val multiAdsSponsoredData = exactMethod(
    "Lcom/facebook/graphql/model/GraphQLFBMultiAdsFeedUnit;",
    "A00",
)

private val partialStorySponsoredData = exactMethod(
    "Lcom/facebook/graphql/model/GraphQLPartialStory;",
    "getSponsoredData",
)

@Suppress("unused")
val blockFacebookAds573Patch = bytecodePatch(
    name = "Block Facebook ads (573)",
    description = "Stops feed, Reels/video, and commercial-break ads without modifying the Story viewer pipeline.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL)

    execute {
        mainFeedTailLoad.method.addInstructions(
            0,
            "return-void",
        )
        mainFeedAsyncAdsTailLoadRunnable.method.addInstructions(
            0,
            "return-void",
        )
        mainFeedAsyncAdsTailLoad.method.addInstructions(
            0,
            "return-void",
        )
        feedAdsResponseConverter.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
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
        asyncFeedAdsController.method.addInstructions(
            0,
            """
                new-instance v0, LX/6Ke;
                invoke-static {}, Lcom/google/common/collect/ImmutableList;->of()Lcom/google/common/collect/ImmutableList;
                move-result-object v1
                const/4 v2, 0x0
                invoke-direct {v0, v1, v2}, LX/6Ke;-><init>(Lcom/google/common/collect/ImmutableList;Ljava/lang/String;)V
                return-object v0
            """.trimIndent(),
        )
        feedEdgeInsertion.method.addInstructions(
            0,
            """
                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->B6k()Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                move-result-object v1
                sget-object v2, Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;->A0K:Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                if-eq v1, v2, :froggo_ads573_drop_feed_edge
                sget-object v2, Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;->A0I:Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                if-eq v1, v2, :froggo_ads573_drop_feed_edge
                goto :froggo_ads573_keep_feed_edge

                :froggo_ads573_drop_feed_edge
                const/4 v0, 0x0
                return v0

                :froggo_ads573_keep_feed_edge
            """.trimIndent(),
        )
        multiAdsSponsoredData.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
        partialStorySponsoredData.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val blockFacebookStoryAds573Patch = bytecodePatch(
    name = "Block Facebook Story ads (573)",
    description = "Disables Facebook's native Story Ads eligibility gate before the Story ad provider is created.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL)

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
