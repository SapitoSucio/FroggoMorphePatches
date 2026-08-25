/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * Validated against the target APK with JADX/MCP and the DEX string table:
 * - AdBucketDataSourceUtil$attemptAdsInsertion$1 -> run(): V
 * - AdBucketDataSourceUtil$attemptFetchMoreAds$1 -> run(): V
 * - AdBucketDataSourceUtil$fetchDeferredAds$1 -> run(): V
 * - AdBucketDataSourceUtil$triggerCtaTailload$1 -> run(): V
 * - AdBucketDataSourceUtil$triggerDwellTailload$1 -> run(): V
 * - MainFeedCSRDataLoaderImpl$handlerTailLoadEvent$2 -> run(): V
 * - MainFeedCSRDataLoaderImpl$maybeDoAsyncAdsTailLoad$1 -> run(): V
 * - MainFeedCSRDataLoaderImpl.maybeDoAsyncAdsTailLoad -> A08(X.1wV): V
 * - FeedCSRAdChannelControllerImpl converter -> X.bZU.A00(...): X.3JJ
 * - X.AuI.Doc(X.9XO): V (ads_rti_insertion bucket insertion)
 * - AdBreakFetchHelper -> A05(...): V
 * - AdBreakStateMachine callback -> onSuccess(Object): V
 * - GraphQLFBMultiAdsFeedUnit.A00(): X.41Q
 * - GraphQLPartialStory.getSponsoredData(): X.41Q
 */
package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
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

private val storyAdsInsertion = redexRunnable(
    "AdBucketDataSourceUtil\$attemptAdsInsertion\$1",
)

private val storyAdsFetchMore = redexRunnable(
    "AdBucketDataSourceUtil\$attemptFetchMoreAds\$1",
)

private val storyAdsDeferredFetch = redexRunnable(
    "AdBucketDataSourceUtil\$fetchDeferredAds\$1",
)

private val storyAdsCtaTailLoad = redexRunnable(
    "AdBucketDataSourceUtil\$triggerCtaTailload\$1",
)

private val storyAdsDwellTailLoad = redexRunnable(
    "AdBucketDataSourceUtil\$triggerDwellTailload\$1",
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

private val storyAdsBucketInsertion = exactMethod(
    "LX/AuI;",
    "Doc",
    listOf("LX/9XO;"),
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
    description = "Stops feed, Story Ads, deferred/tail loads, and video commercial-break ads.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        storyAdsInsertion.method.addInstructions(
            0,
            "return-void",
        )
        storyAdsFetchMore.method.addInstructions(
            0,
            "return-void",
        )
        storyAdsDeferredFetch.method.addInstructions(
            0,
            "return-void",
        )
        storyAdsCtaTailLoad.method.addInstructions(
            0,
            "return-void",
        )
        storyAdsDwellTailLoad.method.addInstructions(
            0,
            "return-void",
        )
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
                const/4 p0, 0x0
                return-object p0
            """.trimIndent(),
        )
        storyAdsBucketInsertion.method.addInstructions(
            0,
            "return-void",
        )
        videoAdBreakFetch.method.addInstructions(
            0,
            "return-void",
        )
        videoAdBreakSuccess.method.addInstructions(
            0,
            "return-void",
        )
        multiAdsSponsoredData.method.addInstructions(
            0,
            """
                const/4 p0, 0x0
                return-object p0
            """.trimIndent(),
        )
        partialStorySponsoredData.method.addInstructions(
            0,
            """
                const/4 p0, 0x0
                return-object p0
            """.trimIndent(),
        )
    }
}
