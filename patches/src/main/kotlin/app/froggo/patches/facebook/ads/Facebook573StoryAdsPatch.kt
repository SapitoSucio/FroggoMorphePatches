package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/*
 * Pre-release only: this file lives on the dev branch and is intentionally
 * absent from main/stable.
 *
 * Facebook 573.0.0.37.74 / 473623755:
 * StoryViewerBucketDataController processes every bucket collection through
 * AkQ.A00(...). That method runs each registered provider's B46(...) in order,
 * including the Story Ads provider (AuI or WXO), then returns the final list to
 * AkQ.A02(...) for publication.
 *
 * The previous experiment forced AmP.A00(...) false. On a cold cache that
 * removes the X68 ads provider from Aky.A0C entirely, so its Ap0 setup, fetch
 * work and ES9 completion/refresh callbacks never participate in the viewer
 * lifecycle. The result blocks ads but can poison Story Viewer publication.
 *
 * This experiment instead leaves the complete provider lifecycle untouched and
 * removes only C9XO from AkQ.A00's final output. C9XO.getBucketType() is always
 * 9 in this APK, so this is a late Story-ad bucket filter after all provider
 * state transitions but before C9TO/viewer publication.
 */
private val storyBucketProcessing = Fingerprint(
    returnType = "Lcom/google/common/collect/ImmutableList;",
    parameters = listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/AkQ;",
        "Lcom/google/common/collect/ImmutableList;",
        "Z",
        "Z",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/AkQ;" && method.name == "A00"
    },
)

@Suppress("unused")
val blockFacebookStoryAds573Patch = bytecodePatch(
    name = "Block Facebook Story ads (573)",
    description = "Pre-release experiment: filters Story ad buckets after provider processing and before viewer publication.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        val targetClass = storyBucketProcessing.classDef
        val targetClassType = targetClass.type
        val filterMethodName = "froggoFilterStoryAds"

        val filterMethod = ImmutableMethod(
            targetClassType,
            filterMethodName,
            listOf(
                ImmutableMethodParameter(
                    "Lcom/google/common/collect/ImmutableList;",
                    null,
                    null,
                ),
            ),
            "Lcom/google/common/collect/ImmutableList;",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(5),
        ).toMutable().apply {
            // This helper is a whole new method whose instructions start at dex
            // offset zero. Keeping all loop labels here avoids non-zero-index
            // label rebasing issues in addInstructions(...).
            addInstructions(
                0,
                """
                    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                    move-result-object v0

                    :froggo_storyads_scan_loop
                    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
                    move-result v1
                    if-eqz v1, :froggo_storyads_filter_exit
                    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                    move-result-object v1
                    instance-of v2, v1, LX/9XO;
                    if-eqz v2, :froggo_storyads_scan_loop

                    new-instance v0, Ljava/util/ArrayList;
                    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                    move-result-object v1

                    :froggo_storyads_filter_loop
                    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z
                    move-result v2
                    if-eqz v2, :froggo_storyads_filter_done
                    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                    move-result-object v2
                    instance-of v3, v2, LX/9XO;
                    if-nez v3, :froggo_storyads_filter_loop
                    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                    goto :froggo_storyads_filter_loop

                    :froggo_storyads_filter_done
                    invoke-static {v0}, Lcom/google/common/collect/ImmutableList;->copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;
                    move-result-object p0

                    :froggo_storyads_filter_exit
                    return-object p0
                """.trimIndent(),
            )
        }

        targetClass.methods.add(filterMethod)

        val returnIndex = storyBucketProcessing.method.implementation!!.instructions
            .withIndex()
            .last { (_, instruction) -> instruction.opcode == Opcode.RETURN_OBJECT }
            .index

        storyBucketProcessing.method.addInstructions(
            returnIndex,
            """
                invoke-static {p2}, $targetClassType->$filterMethodName(Lcom/google/common/collect/ImmutableList;)Lcom/google/common/collect/ImmutableList;
                move-result-object p2
            """.trimIndent(),
        )
    }
}
