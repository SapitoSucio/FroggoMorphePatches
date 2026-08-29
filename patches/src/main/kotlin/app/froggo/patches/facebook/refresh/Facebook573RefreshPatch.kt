/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * refreshForRevisit(...): Z is shared by four callers:
 * - NewsFeedFragment.A0n() -> "onAppForeground"
 * - NewsFeedFragment.onResume() -> "onResume"
 * - NewsFeedFragment.onActivityResult() -> "onActivityResult"
 * - fullscreen-video close -> "FullscreenVideoViewCloseEvent"
 *
 * The old patch modified X.2UL.refreshForRevisit itself and also replaced
 * X.2UL.A0A(...), the stale-post/rerank decision executor, with return-void.
 * Besides suppressing unrelated tab/pause decisions, modifying the large
 * shared refreshForRevisit method caused VerifyError crashes on some devices.
 *
 * Keep X.2UL completely stock. Only bypass the two lifecycle call sites that
 * initiate automatic revisit refreshes. Activity-result, fullscreen close,
 * tab switching, stale-post decisions and manual refresh remain untouched.
 */
package app.froggo.patches.facebook.refresh

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val newsFeedOnAppForeground = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        classDef.type == "LX/2Q7;" && method.name == "A0n"
    },
)

private val newsFeedOnResume = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        classDef.type == "LX/2Q7;" && method.name == "onResume"
    },
)

@Suppress("unused")
val blockFacebookAutomaticRefresh573Patch = bytecodePatch(
    name = "Block Facebook automatic refresh (573)",
    description = "Experimental: blocks only foreground/onResume feed revisit refresh while preserving manual, tab, activity-result and fullscreen refresh paths.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL)

    execute {
        val foregroundInstructions = newsFeedOnAppForeground.method.implementation!!.instructions
        val foregroundRefreshCalls = foregroundInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (reference?.definingClass == "LX/2UL;" && reference.name == "refreshForRevisit") index else null
        }
        require(foregroundRefreshCalls.size == 1) {
            "Expected exactly one refreshForRevisit call in NewsFeedFragment.A0n"
        }
        val foregroundCallIndex = foregroundRefreshCalls.single()
        val foregroundCall = foregroundInstructions[foregroundCallIndex] as? RegisterRangeInstruction
            ?: error("Expected range invoke for NewsFeedFragment.A0n refreshForRevisit")
        require(foregroundCall.startRegister == 1 && foregroundCall.registerCount == 7) {
            "Unexpected NewsFeedFragment.A0n refreshForRevisit register range"
        }

        // Keep refreshForRevisit stock, but neutralize its explicit-refresh gate.
        // In this callsite z/z2/z3 are already false; forcing z4=false makes the
        // method return false without starting a revisit refresh.
        newsFeedOnAppForeground.method.addInstructions(
            foregroundCallIndex,
            "const/4 v7, 0x0",
        )

        val resumeInstructions = newsFeedOnResume.method.implementation!!.instructions
        val resumeRefreshCalls = resumeInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (reference?.definingClass == "LX/2UL;" && reference.name == "refreshForRevisit") index else null
        }
        require(resumeRefreshCalls.size == 1) {
            "Expected exactly one refreshForRevisit call in NewsFeedFragment.onResume"
        }
        val resumeCallIndex = resumeRefreshCalls.single()
        val resumeCall = resumeInstructions[resumeCallIndex] as? RegisterRangeInstruction
            ?: error("Expected range invoke for NewsFeedFragment.onResume refreshForRevisit")
        require(resumeCall.startRegister == 15 && resumeCall.registerCount == 7) {
            "Unexpected NewsFeedFragment.onResume refreshForRevisit register range"
        }

        // Keep the stock invoke + move-result sequence. Setting z=false and z4=false
        // makes the normal refreshForRevisit path return false without refreshing,
        // while avoiding labels, register remapping or edits inside X.2UL itself.
        newsFeedOnResume.method.addInstructions(
            resumeCallIndex,
            """
                move/from16 v16, v2
                move/from16 v21, v2
            """.trimIndent(),
        )
    }
}
