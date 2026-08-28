package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

/*
 * DEV-ONLY diagnostic split for Facebook 573 ads.
 *
 * Keep Block Facebook ads (573) disabled while testing these patches. Each
 * diagnostic patch owns a disjoint subset of the stable patch so runtime A/B
 * tests can identify the exact family that poisons Story viewer state.
 */
private fun diagRedexRunnable(originalName: String) = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        method.name == "run" && classDef.fields.any { field ->
            field.name == "__redex_internal_original_name" &&
                (field.initialValue as? StringEncodedValue)?.value == originalName
        }
    },
)

private fun diagExactMethod(
    classDescriptor: String,
    methodName: String,
    parameters: List<String> = emptyList(),
) = Fingerprint(
    parameters = parameters,
    custom = { method, classDef ->
        classDef.type == classDescriptor && method.name == methodName
    },
)

private val diagMainFeedAsyncAdsTailLoadRunnable = diagRedexRunnable(
    "MainFeedCSRDataLoaderImpl\$maybeDoAsyncAdsTailLoad\$1",
)

private val diagMainFeedAsyncAdsTailLoad = diagExactMethod(
    "LX/1wV;",
    "A08",
    listOf("LX/1wV;"),
)

private val diagFeedAdsResponseConverter = diagExactMethod(
    "LX/bZU;",
    "A00",
    listOf(
        "Lcom/facebook/api/feed/model/FetchFeedParams;",
        "LX/3pN;",
        "LX/41R;",
    ),
)

private val diagAsyncFeedAdsController = diagExactMethod(
    "LX/3JX;",
    "A0F",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/3pN;",
        "Lcom/facebook/graphql/executor/GraphQLResult;",
    ),
)

private val diagFeedEdgeInsertion = diagExactMethod(
    "LX/1vv;",
    "addNewEdgeToCollection",
    listOf(
        "Lcom/google/common/collect/ImmutableList\$Builder;",
        "Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;",
        "LX/1cP;",
    ),
)

private val diagMultiAdsSponsoredData = diagExactMethod(
    "Lcom/facebook/graphql/model/GraphQLFBMultiAdsFeedUnit;",
    "A00",
)

private val diagVideoAdBreakFetch = diagExactMethod(
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

private val diagVideoAdBreakSuccess = diagExactMethod(
    "LX/Qsw;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val diagReelsVideoAdFetch = diagExactMethod(
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

private val diagReelsBannerAdSuccess = diagExactMethod(
    "LX/62B;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val diagReelsVideoAdSuccess = diagExactMethod(
    "LX/9mO;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val diagStoryFragmentCreate = diagExactMethod(
    "LX/Aky;",
    "onFragmentCreate",
    listOf("Landroid/os/Bundle;"),
)

private val diagStoryPublish = diagExactMethod(
    "LX/AkQ;",
    "A02",
    listOf(
        "LX/Alw;",
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/AkQ;",
        "Lcom/google/common/collect/ImmutableList;",
        "Z",
    ),
)

private val diagStoryPublishRunnable = diagExactMethod(
    "LX/Am0;",
    "run",
)

private val diagStoryCachedReplay = diagExactMethod(
    "LX/AkQ;",
    "A03",
    listOf("LX/Alw;", "LX/Bsm;", "LX/AkQ;"),
)

private val diagStoryNotify = diagExactMethod(
    "LX/Al1;",
    "A00",
    listOf("LX/Alw;", "LX/Bsm;"),
)

@Suppress("unused")
val diagnoseFacebookAds573A1TailLoadPatch = bytecodePatch(
    name = "[Diag A1] Facebook 573 ads - CSR tail-load",
    description = "DEV diagnostic: only disables MainFeedCSRDataLoaderImpl async-ad tail-load dispatch.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagMainFeedAsyncAdsTailLoadRunnable.method.addInstructions(0, "return-void")
        diagMainFeedAsyncAdsTailLoad.method.addInstructions(0, "return-void")
    }
}

@Suppress("unused")
val diagnoseFacebookAds573A2ConverterPatch = bytecodePatch(
    name = "[Diag A2] Facebook 573 ads - CSR converter",
    description = "DEV diagnostic: only nulls the bZU Feed CSR response converter.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagFeedAdsResponseConverter.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val diagnoseFacebookAds573A3AsyncControllerPatch = bytecodePatch(
    name = "[Diag A3] Facebook 573 ads - Async controller",
    description = "DEV diagnostic: only replaces FeedAsyncAdsController output with an empty C6Ke.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagAsyncFeedAdsController.method.addInstructions(
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
    }
}

@Suppress("unused")
val diagnoseFacebookAds573BFinalFeedPatch = bytecodePatch(
    name = "[Diag B] Facebook 573 ads - Final feed filter",
    description = "DEV diagnostic: only filters SPONSORED/PROMOTION feed edges and MultiAds sponsored data.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagFeedEdgeInsertion.method.addInstructions(
            0,
            """
                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->B6k()Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                move-result-object v1
                sget-object v2, Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;->A0K:Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                if-eq v1, v2, :froggo_diag_ads573_drop_feed_edge
                sget-object v2, Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;->A0I:Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                if-eq v1, v2, :froggo_diag_ads573_drop_feed_edge
                goto :froggo_diag_ads573_keep_feed_edge

                :froggo_diag_ads573_drop_feed_edge
                const/4 v0, 0x0
                return v0

                :froggo_diag_ads573_keep_feed_edge
            """.trimIndent(),
        )
        diagMultiAdsSponsoredData.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val diagnoseFacebookAds573CVideoReelsPatch = bytecodePatch(
    name = "[Diag C] Facebook 573 ads - Reels/video",
    description = "DEV diagnostic: only disables Reels/video and commercial-break ad fetch/success paths.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagVideoAdBreakFetch.method.addInstructions(0, "return-void")
        diagVideoAdBreakSuccess.method.addInstructions(0, "return-void")
        diagReelsVideoAdFetch.method.addInstructions(0, "return-void")
        diagReelsBannerAdSuccess.method.addInstructions(0, "return-void")
        diagReelsVideoAdSuccess.method.addInstructions(0, "return-void")
    }
}

@Suppress("unused")
val diagnoseFacebookAds573DStoryPublicationPatch = bytecodePatch(
    name = "[Diag D] Facebook 573 Stories - publication lifecycle",
    description = "DEV diagnostic: logs Story bucket publication, cached replay and viewer notification without changing behavior.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagStoryFragmentCreate.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "FRAGMENT_CREATE"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
        diagStoryPublish.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "AKQ_A02_PUBLISH"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
        diagStoryPublishRunnable.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "AM0_RUN"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
        diagStoryCachedReplay.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "AKQ_REPLAY_CACHED"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
        diagStoryNotify.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "AL1_NOTIFY"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
    }
}
