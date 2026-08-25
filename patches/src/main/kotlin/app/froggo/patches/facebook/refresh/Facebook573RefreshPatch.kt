/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * NewsFeedFragmentDataController$maybeRefreshForHotStart$1 only sets a flag;
 * the actual automatic refresh is requested through
 * NewsFeedFragmentDataController.refreshForRevisit(...): Z. On foreground,
 * NewsFeedFragment$onAppForeground$1 and $2 can also call
 * NewsFeedFragment.A0N ("foreground" / "foreground_ads"), so both automatic
 * foreground callbacks are patched. The separate handlePTRRefresh$1 callback
 * and C2UL.A0M refresh callers are intentionally not patched, so manual
 * pull-to-refresh remains available.
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

private val automaticRefreshForRevisit = Fingerprint(
    returnType = "Z",
    parameters = listOf(
        "Z",
        "Z",
        "Z",
        "I",
        "Ljava/lang/String;",
        "Z",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/2UL;" && method.name == "refreshForRevisit"
    },
)

private val automaticForegroundRefresh = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        method.name == "run" && classDef.fields.any { field ->
            field.name == "__redex_internal_original_name" &&
                ((field.initialValue as? StringEncodedValue)?.value ==
                    "NewsFeedFragment\$onAppForeground\$1" ||
                    (field.initialValue as? StringEncodedValue)?.value ==
                    "NewsFeedFragment\$onAppForeground\$2")
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
        automaticRefreshForRevisit.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        automaticForegroundRefresh.method.addInstructions(
            0,
            "return-void",
        )
    }
}
