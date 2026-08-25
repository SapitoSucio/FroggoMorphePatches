/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * Validated against the target APK with JADX/MCP and the DEX string table:
 * - AdBucketDataSourceUtil$attemptAdsInsertion$1 -> run(): V
 * - AdBucketDataSourceUtil$attemptFetchMoreAds$1 -> run(): V
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

private fun exactMethod(classDescriptor: String, methodName: String) = Fingerprint(
    parameters = emptyList(),
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
