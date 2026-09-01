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
        // navigates to the current card with Facebook's AUTO_LOOP reason; otherwise
        // returning preserves the completed frame and manual navigation remains intact.
        if (loopStoriesOption.value == true) {
            storyProgressCompletion.method.addInstructions(
                autoAdvanceCalls.single(),
                """
                    invoke-virtual {p1}, Lcom/facebook/stories/model/StoryBucket;->A0H()Lcom/google/common/collect/ImmutableList;
                    move-result-object v0
                    invoke-virtual {v0, p2}, Lcom/google/common/collect/ImmutableList;->indexOf(Ljava/lang/Object;)I
                    move-result p3
                    if-ltz p3, :froggo_story_loop_done
                    iget-object v0, p0, LX/9UW;->A03:LX/COW;
                    sget-object p1, LX/9Tl;->A05:LX/9Tl;
                    invoke-interface {v0, p1, p3}, LX/COW;->Cui(LX/9Tl;I)V
                    const/4 v0, 0x0
                    iput v0, p0, LX/9UW;->A00:I
                    :froggo_story_loop_done
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
