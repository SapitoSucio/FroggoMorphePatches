package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/*
 * Facebook 573.0.0.37.74 / 473623755:
 *
 * AmP=false removed the X68 Story Ads provider entirely and broke its lifecycle.
 * The previous provider-boundary approach preserved X68 but injected a helper
 * call into AkQ.A00 after every provider B46(...), including the initial
 * AkS/AkT/AkV pass that runs before X68 exists. Cold-start logging showed that
 * the first Story can fail before deferred/X68 initialization, so this variant
 * leaves AkQ.A00 completely stock.
 *
 * AuI and WXO are the two concrete X68 implementations. This patch lets
 * each provider execute its stock B46(...) fully, then removes only C9XO from
 * the provider's own normal return value. Downstream AkT/AkV/WUB therefore see
 * an ad-free collection, while the early AkQ publication path is untouched.
 */
private val storyAdsAuI = Fingerprint(
    returnType = "Lcom/google/common/collect/ImmutableList;",
    parameters = listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/Aly;",
        "Lcom/google/common/collect/ImmutableList;",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/AuI;" && method.name == "B46"
    },
)

private val storyAdsWXO = Fingerprint(
    returnType = "Lcom/google/common/collect/ImmutableList;",
    parameters = listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/Aly;",
        "Lcom/google/common/collect/ImmutableList;",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/WXO;" && method.name == "B46"
    },
)

@Suppress("unused")
val blockFacebookStoryAds573Patch = bytecodePatch(
    name = "Block Facebook Story ads (573)",
    description = "Filters Story ad buckets only at the concrete X68 provider return boundary.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        val filterMethodName = "froggoFilterStoryAdsOutput"
        val filterInstructions =
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
            """.trimIndent()

        val auIClass = storyAdsAuI.classDef
        val auIType = auIClass.type
        val auIFilterMethod = ImmutableMethod(
            auIType,
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
            addInstructions(0, filterInstructions)
        }
        auIClass.methods.add(auIFilterMethod)

        val auIInstructions = storyAdsAuI.method.implementation!!.instructions
        val auIReturnIndex = auIInstructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.RETURN_OBJECT
        }
        require(auIReturnIndex >= 0) { "Could not find AuI.B46 normal return" }
        val auIReturnRegister = (auIInstructions[auIReturnIndex] as? OneRegisterInstruction)?.registerA
            ?: error("Unexpected AuI.B46 return instruction")
        require(auIReturnRegister <= 15) { "AuI.B46 return register cannot be encoded in invoke-static" }

        storyAdsAuI.method.addInstructions(
            auIReturnIndex,
            """
                invoke-static {v$auIReturnRegister}, $auIType->$filterMethodName(Lcom/google/common/collect/ImmutableList;)Lcom/google/common/collect/ImmutableList;
                move-result-object v$auIReturnRegister
            """.trimIndent(),
        )

        val wxoClass = storyAdsWXO.classDef
        val wxoType = wxoClass.type
        val wxoFilterMethod = ImmutableMethod(
            wxoType,
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
            addInstructions(0, filterInstructions)
        }
        wxoClass.methods.add(wxoFilterMethod)

        val wxoInstructions = storyAdsWXO.method.implementation!!.instructions
        val wxoReturnIndex = wxoInstructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.RETURN_OBJECT
        }
        require(wxoReturnIndex >= 0) { "Could not find WXO.B46 return" }
        val wxoReturnRegister = (wxoInstructions[wxoReturnIndex] as? OneRegisterInstruction)?.registerA
            ?: error("Unexpected WXO.B46 return instruction")
        require(wxoReturnRegister <= 15) { "WXO.B46 return register cannot be encoded in invoke-static" }

        storyAdsWXO.method.addInstructions(
            wxoReturnIndex,
            """
                invoke-static {v$wxoReturnRegister}, $wxoType->$filterMethodName(Lcom/google/common/collect/ImmutableList;)Lcom/google/common/collect/ImmutableList;
                move-result-object v$wxoReturnRegister
            """.trimIndent(),
        )
    }
}
