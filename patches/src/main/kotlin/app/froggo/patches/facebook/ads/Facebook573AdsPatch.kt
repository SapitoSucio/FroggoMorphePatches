package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

/*
 * Facebook 573.0.0.37.74 / 473623755 - Feed ads only.
 *
 * Proven seams:
 * - MainFeedCSRDataLoaderImpl$maybeDoAsyncAdsTailLoad$1 -> run(): V
 * - MainFeedCSRDataLoaderImpl.maybeDoAsyncAdsTailLoad -> X.1wV.A08(...): V
 * - FeedCSRAdChannelControllerImpl converter -> X.bZU.A00(...): X.3JJ
 * - FeedAsyncAdsController -> X.3JX.A0F(...): X.6Ke
 * - X.1vv.addNewEdgeToCollection(...): final SPONSORED/PROMOTION edge guard
 * - GraphQLFBMultiAdsFeedUnit.A00(): sponsored-data fallback
 *
 * Reels/video/commercial-break blocking intentionally lives in the separate
 * Block Facebook Reels ads (573) patch. Story Ads have their own patch too.
 */
private fun feedRedexRunnable(originalName: String) = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        method.name == "run" && classDef.fields.any { field ->
            field.name == "__redex_internal_original_name" &&
                (field.initialValue as? StringEncodedValue)?.value == originalName
        }
    },
)

private fun feedExactMethod(
    classDescriptor: String,
    methodName: String,
    parameters: List<String> = emptyList(),
) = Fingerprint(
    parameters = parameters,
    custom = { method, classDef ->
        classDef.type == classDescriptor && method.name == methodName
    },
)

private val mainFeedAsyncAdsTailLoadRunnable = feedRedexRunnable(
    "MainFeedCSRDataLoaderImpl\$maybeDoAsyncAdsTailLoad\$1",
)

private val mainFeedAsyncAdsTailLoad = feedExactMethod(
    "LX/1wV;",
    "A08",
    listOf("LX/1wV;"),
)

private val feedAdsResponseConverter = feedExactMethod(
    "LX/bZU;",
    "A00",
    listOf(
        "Lcom/facebook/api/feed/model/FetchFeedParams;",
        "LX/3pN;",
        "LX/41R;",
    ),
)

private val asyncFeedAdsController = feedExactMethod(
    "LX/3JX;",
    "A0F",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/3pN;",
        "Lcom/facebook/graphql/executor/GraphQLResult;",
    ),
)

private val multiAdsSponsoredData = feedExactMethod(
    "Lcom/facebook/graphql/model/GraphQLFBMultiAdsFeedUnit;",
    "A00",
)

private val feedEdgeInsertion = feedExactMethod(
    "LX/1vv;",
    "addNewEdgeToCollection",
    listOf(
        "Lcom/google/common/collect/ImmutableList\$Builder;",
        "Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;",
        "LX/1cP;",
    ),
)

@Suppress("unused")
val blockFacebookFeedAds573Patch = bytecodePatch(
    name = "Block Facebook Feed ads (573)",
    description = "Blocks sponsored and promoted units in the Facebook 573 Feed without touching Reels or Stories.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        mainFeedAsyncAdsTailLoadRunnable.method.addInstructions(
            0,
            "return-void",
        )
        mainFeedAsyncAdsTailLoad.method.addInstructions(
            0,
            "return-void",
        )
        feedAdsResponseConverter.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
        asyncFeedAdsController.method.addInstructions(
            0,
            """
                new-instance v0, LX/6Ke;
                invoke-static {}, Lcom/google/common/collect/ImmutableList;->of()Lcom/google/common/collect/ImmutableList;
                move-result-object v1
                const/4 v2, 0x0
                invoke-direct {v0, v1, v2}, LX/6Ke;-><init>(Lcom/google/common/collect/ImmutableList;Ljava/lang/String;)V
                return-object v0
            """.trimIndent(),
        )
        val edgeMethod = feedEdgeInsertion.method
        val edgeBody = requireNotNull(edgeMethod.implementation)
        require(edgeMethod.returnType == "Z" && !AccessFlags.STATIC.isSet(edgeMethod.accessFlags)) {
            "Unexpected Facebook Feed edge insertion signature"
        }
        require(edgeBody.registerCount >= 7) { "Feed edge guard needs three local registers" }
        val category = "Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;"

        // Bind targets to this method's locations, so later AI-filter prepends rebase them.
        val keepEdge = edgeBody.newLabelForIndex(0)
        val dropInstructions = listOf(
            BuilderInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference("FroggoFeedAds573")),
            BuilderInstruction21c(Opcode.CONST_STRING, 1, ImmutableStringReference("blocked SPONSORED/PROMOTION")),
            BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 0, 1, 0, 0, 0,
                ImmutableMethodReference("Landroid/util/Log;", "d", listOf("Ljava/lang/String;", "Ljava/lang/String;"), "I")),
            BuilderInstruction11n(Opcode.CONST_4, 0, 0),
            BuilderInstruction11x(Opcode.RETURN, 0),
        )
        dropInstructions.asReversed().forEach { edgeBody.addInstruction(0, it) }
        val dropEdge = edgeBody.newLabelForIndex(0)
        val guardInstructions = listOf(
            BuilderInstruction22x(Opcode.MOVE_OBJECT_FROM16, 0, edgeBody.registerCount - 2),
            BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0,
                ImmutableMethodReference("Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;", "B6k", emptyList(), category)),
            BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1),
            BuilderInstruction21c(Opcode.SGET_OBJECT, 2, ImmutableFieldReference(category, "A0K", category)),
            BuilderInstruction22t(Opcode.IF_EQ, 1, 2, dropEdge),
            BuilderInstruction21c(Opcode.SGET_OBJECT, 2, ImmutableFieldReference(category, "A0I", category)),
            BuilderInstruction22t(Opcode.IF_EQ, 1, 2, dropEdge),
            BuilderInstruction10t(Opcode.GOTO, keepEdge),
        )
        guardInstructions.asReversed().forEach { edgeBody.addInstruction(0, it) }
        multiAdsSponsoredData.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
    }
}
