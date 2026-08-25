/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * Validated against the loaded APK with JADX/MCP and the DEX string table:
 * - AdBucketDataSource$attemptAdsInsertion$1 -> run(): V
 * - AdBucketDataSource$attemptFetchMoreAds$1 -> run(): V
 * - GraphQLFBMultiAdsFeedUnit.A00(): X.41Q
 * - GraphQLPartialStory.getSponsoredData(): X.41Q
 *
 * The first two hooks stop Story Ads before insertion/fetch. The GraphQL hooks
 * are a late safeguard for sponsored feed units in this universal APK.
 */
package app.morphe.patches.facebook.ads.v573

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
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

private fun exactMethod(classDescriptor: String, methodName: String) = Fingerprint(
    parameters = emptyList(),
    custom = { method, classDef ->
        classDef.type == classDescriptor && method.name == methodName
    },
)

private val storyAdsInsertion = redexRunnable(
    "AdBucketDataSource\$attemptAdsInsertion\$1",
)

private val storyAdsFetchMore = redexRunnable(
    "AdBucketDataSource\$attemptFetchMoreAds\$1",
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
    description = "Stops Story Ads insertion/fetch and removes sponsored-data access in the feed.",
) {
    compatibleWith(AppCompatibilities.FACEBOOK)

    execute {
        storyAdsInsertion.method.returnEarly()
        storyAdsFetchMore.method.returnEarly()
        multiAdsSponsoredData.method.returnEarly()
        partialStorySponsoredData.method.returnEarly()
    }
}
