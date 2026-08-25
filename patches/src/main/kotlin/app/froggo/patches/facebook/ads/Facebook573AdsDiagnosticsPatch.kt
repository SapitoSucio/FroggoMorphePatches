/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * Opt-in diagnostics for locating ad delivery/rendering paths. This patch does
 * not change control flow or dump request data; it only writes fixed route
 * labels to Android Logcat.
 */
package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
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

private val feedTailLoad = redexRunnable(
    "MainFeedCSRDataLoaderImpl\$maybeDoAsyncAdsTailLoad\$1",
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

private val feedAdsChannel = exactMethod(
    "LX/23I;",
    "A0F",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/3pN;",
        "Lcom/facebook/graphql/executor/GraphQLResult;",
    ),
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

private fun Fingerprint.logRoute(label: String) {
    val method = this.method
    val register = runCatching {
        method.getFreeRegisterProvider(0, 1, emptyList()).getFreeRegister()
    }.getOrNull() ?: return

    // Log.d uses a 4-bit register list. Skip a route rather than producing an
    // invalid patch if this method has no safe low register available.
    if (register >= 16) return

    method.addInstructions(
        0,
        """
            const-string v$register, "$label"
            invoke-static {v$register, v$register}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent(),
    )
}

@Suppress("unused")
val logFacebookAdsRoutes573Patch = bytecodePatch(
    name = "Log Facebook ad routes (573)",
    description = "Opt-in Logcat route markers for feed, Reels/video, and Story Ads diagnostics.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        feedTailLoad.logRoute("FroggoAds573/ftail")
        storyAdsInsertion.logRoute("FroggoAds573/sins")
        storyAdsFetchMore.logRoute("FroggoAds573/sfetch")
        storyAdsDeferredFetch.logRoute("FroggoAds573/sdefer")
        feedAdsChannel.logRoute("FroggoAds573/fchan")
        feedAdsResponseConverter.logRoute("FroggoAds573/fconv")
        storyAdsBucketInsertion.logRoute("FroggoAds573/sbucket")
        videoAdBreakFetch.logRoute("FroggoAds573/vfetch")
        videoAdBreakSuccess.logRoute("FroggoAds573/vok")
        multiAdsSponsoredData.logRoute("FroggoAds573/gmulti")
        partialStorySponsoredData.logRoute("FroggoAds573/gstory")
    }
}
