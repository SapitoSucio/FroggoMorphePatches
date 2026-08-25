/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * NewsFeedFragmentDataController$maybeRefreshForHotStart$1 is a synthetic
 * Runnable.run(): V. The separate handlePTRRefresh$1 callback is intentionally
 * not patched, so manual pull-to-refresh remains available.
 */
package app.froggo.patches.facebook.refresh

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

private val automaticHotStartRefresh = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        method.name == "run" && classDef.fields.any { field ->
            field.name == "__redex_internal_original_name" &&
                (field.initialValue as? StringEncodedValue)?.value ==
                "NewsFeedFragmentDataController\$maybeRefreshForHotStart\$1"
        }
    },
)

@Suppress("unused")
val blockFacebookAutomaticRefresh573Patch = bytecodePatch(
    name = "Block Facebook automatic refresh (573)",
    description = "Disables News Feed hot-start refresh while preserving manual pull-to-refresh.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        automaticHotStartRefresh.method.addInstructions(
            0,
            "return-void",
        )
    }
}
