package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

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
        val returnIndex = storyBucketProcessing.method.implementation!!.instructions
            .withIndex()
            .last { (_, instruction) -> instruction.opcode == Opcode.RETURN_OBJECT }
            .index

        storyBucketProcessing.method.addInstructions(
            returnIndex,
            """
                invoke-interface {p2}, Ljava/util/List;->iterator()Ljava/util/Iterator;
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
                invoke-interface {p2}, Ljava/util/List;->iterator()Ljava/util/Iterator;
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
                move-result-object v0
                move-object p2, v0

                :froggo_storyads_filter_exit
            """.trimIndent(),
        )
    }
}
