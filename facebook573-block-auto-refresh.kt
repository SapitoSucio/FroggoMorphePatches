/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * Validated against the loaded APK's DEX with JADX/MCP support:
 * NewsFeedFragmentDataController$maybeRefreshForHotStart$1 has a synthetic
 * Runnable.run(): V and the Redex original-name field below.
 *
 * This blocks the hot-start automatic refresh only. The separate
 * handlePTRRefresh$1 callback is intentionally not patched, so manual
 * pull-to-refresh remains available.
 */
package app.morphe.patches.facebook.refresh.v573

import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
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
) {
    compatibleWith(AppCompatibilities.FACEBOOK)

    execute {
        automaticHotStartRefresh.method.returnEarly()
    }
}
