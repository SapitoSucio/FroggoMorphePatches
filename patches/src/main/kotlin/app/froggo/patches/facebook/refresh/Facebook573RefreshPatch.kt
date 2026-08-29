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
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
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

        // The return value is unused in A0n(), so jump over the stock call only.
        newsFeedOnAppForeground.method.addInstructions(
            foregroundCallIndex + 1,
            ":froggo_refresh573_after_app_foreground",
        )
        newsFeedOnAppForeground.method.addInstructions(
            foregroundCallIndex,
            "goto :froggo_refresh573_after_app_foreground",
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
        require(resumeInstructions[resumeCallIndex + 1].opcode == Opcode.MOVE_RESULT) {
            "Unexpected refreshForRevisit result sequence in NewsFeedFragment.onResume"
        }
        val resumeResultRegister =
            (resumeInstructions[resumeCallIndex + 1] as? OneRegisterInstruction)?.registerA
                ?: error("Could not resolve onResume refresh result register")

        // onResume consumes the boolean result after the call. Preserve that control
        // flow by supplying false, then jump over both invoke + move-result.
        newsFeedOnResume.method.addInstructions(
            resumeCallIndex + 2,
            ":froggo_refresh573_after_on_resume",
        )
        newsFeedOnResume.method.addInstructions(
            resumeCallIndex,
            """
                const/4 v$resumeResultRegister, 0x0
                goto :froggo_refresh573_after_on_resume
            """.trimIndent(),
        )
    }
}
