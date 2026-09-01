package app.froggo.patches.facebook.stories

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.booleanOption
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val storyProgressCompletion = Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Lcom/facebook/stories/model/StoryBucket;",
        "Lcom/facebook/stories/model/StoryCard;",
        "I",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/9UW;" && method.name == "Dj5"
    },
)

@Suppress("unused")
val stopFacebookStoryAutoAdvance573Patch = bytecodePatch(
    name = "Stop Facebook Story auto-advance (573)",
    description = "Leaves photo and video Stories on their completed frame until the viewer navigates manually.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    val loopStoriesOption = booleanOption(
        key = "loopStories",
        default = false,
        title = "Loop Stories",
        description = "When enabled, each completed Story restarts instead of advancing to the next Story.",
    )

    execute {
        val instructions = storyProgressCompletion.method.implementation!!.instructions
        val autoAdvanceCalls = instructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                reference?.definingClass == "LX/9UW;" &&
                    reference.name == "A00" &&
                    reference.parameterTypes.size == 1 &&
                    reference.parameterTypes.first() == "Lcom/facebook/stories/model/StoryCard;"
            ) {
                index
            } else {
                null
            }
        }
        require(autoAdvanceCalls.size == 1) {
            "Expected exactly one Story auto-advance call in C9UW.Dj5"
        }

        // C9UW reaches this call only after the broadcaster reports 1000. Looping
        // briefly interrupts playback, broadcasts Facebook's stock reset event,
        // then resumes so the progress controller registers its timer again.
        if (loopStoriesOption.value == true) {
            storyProgressCompletion.method.addInstructions(
                autoAdvanceCalls.single(),
                """
                    invoke-virtual {p0}, LX/BsK;->A0A()LX/BsZ;
                    move-result-object v0
                    invoke-static {v0}, LX/BsZ;->A0B(LX/BsZ;)LX/9TX;
                    move-result-object v0
                    sget-object p1, LX/9Tg;->A1O:LX/9Tg;
                    invoke-interface {v0, p1}, LX/9TX;->Euy(LX/9Tg;)V
                    const/4 p2, 0x1
                    invoke-interface {v0, p2}, LX/9TX;->ESy(Z)V
                    invoke-interface {v0, p1}, LX/9TX;->AlV(LX/9Tg;)V
                    const/4 p2, 0x0
                    iput p2, p0, LX/9UW;->A00:I
                    return-void
                """.trimIndent(),
            )
        } else {
            storyProgressCompletion.method.addInstructions(
                autoAdvanceCalls.single(),
                "return-void",
            )
        }
    }
}
