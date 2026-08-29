package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/*
 * DEV-ONLY diagnostic split for Facebook 573 ads.
 *
 * Keep Block Facebook ads (573) disabled while testing these patches. Each
 * diagnostic patch owns a disjoint subset of the stable patch so runtime A/B
 * tests can identify the exact family that poisons Story viewer state.
 */
private fun diagRedexRunnable(originalName: String) = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        method.name == "run" && classDef.fields.any { field ->
            field.name == "__redex_internal_original_name" &&
                (field.initialValue as? StringEncodedValue)?.value == originalName
        }
    },
)

private fun diagExactMethod(
    classDescriptor: String,
    methodName: String,
    parameters: List<String> = emptyList(),
) = Fingerprint(
    parameters = parameters,
    custom = { method, classDef ->
        classDef.type == classDescriptor && method.name == methodName
    },
)

private val diagMainFeedAsyncAdsTailLoadRunnable = diagRedexRunnable(
    "MainFeedCSRDataLoaderImpl\$maybeDoAsyncAdsTailLoad\$1",
)

private val diagMainFeedAsyncAdsTailLoad = diagExactMethod(
    "LX/1wV;",
    "A08",
    listOf("LX/1wV;"),
)

private val diagFeedAdsResponseConverter = diagExactMethod(
    "LX/bZU;",
    "A00",
    listOf(
        "Lcom/facebook/api/feed/model/FetchFeedParams;",
        "LX/3pN;",
        "LX/41R;",
    ),
)

private val diagAsyncFeedAdsController = diagExactMethod(
    "LX/3JX;",
    "A0F",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/3pN;",
        "Lcom/facebook/graphql/executor/GraphQLResult;",
    ),
)

private val diagFeedEdgeInsertion = diagExactMethod(
    "LX/1vv;",
    "addNewEdgeToCollection",
    listOf(
        "Lcom/google/common/collect/ImmutableList\$Builder;",
        "Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;",
        "LX/1cP;",
    ),
)

private val diagMultiAdsSponsoredData = diagExactMethod(
    "Lcom/facebook/graphql/model/GraphQLFBMultiAdsFeedUnit;",
    "A00",
)

private val diagVideoAdBreakFetch = diagExactMethod(
    "LX/Qsb;",
    "A05",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/41Q;",
        "LX/4ta;",
        "I",
        "Z",
        "Z",
    ),
)

private val diagVideoAdBreakSuccess = diagExactMethod(
    "LX/Qsw;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val diagReelsVideoAdFetch = diagExactMethod(
    "LX/5Vs;",
    "A03",
    listOf(
        "LX/5Vw;",
        "LX/41Q;",
        "LX/caj;",
        "LX/5I6;",
        "Ljava/lang/Boolean;",
        "Ljava/lang/Integer;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "I",
        "I",
        "J",
        "Z",
        "Z",
        "Z",
    ),
)

private val diagReelsBannerAdSuccess = diagExactMethod(
    "LX/62B;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val diagReelsVideoAdSuccess = diagExactMethod(
    "LX/9mO;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val diagStoryFragmentCreate = diagExactMethod(
    "LX/Aky;",
    "onFragmentCreate",
    listOf("Landroid/os/Bundle;"),
)

private val diagStoryPublish = diagExactMethod(
    "LX/AkQ;",
    "A02",
    listOf(
        "LX/Alw;",
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/AkQ;",
        "Lcom/google/common/collect/ImmutableList;",
        "Z",
    ),
)

private val diagStoryPublishRunnable = diagExactMethod(
    "LX/Am0;",
    "run",
)

private val diagStoryCachedReplay = diagExactMethod(
    "LX/AkQ;",
    "A03",
    listOf("LX/Alw;", "LX/Bsm;", "LX/AkQ;"),
)

private val diagStoryNotify = diagExactMethod(
    "LX/Al1;",
    "A00",
    listOf("LX/Alw;", "LX/Bsm;"),
)

@Suppress("unused")
val diagnoseFacebookAds573A1TailLoadPatch = bytecodePatch(
    name = "[Diag A1] Facebook 573 ads - CSR tail-load",
    description = "DEV diagnostic: only disables MainFeedCSRDataLoaderImpl async-ad tail-load dispatch.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagMainFeedAsyncAdsTailLoadRunnable.method.addInstructions(0, "return-void")
        diagMainFeedAsyncAdsTailLoad.method.addInstructions(0, "return-void")
    }
}

@Suppress("unused")
val diagnoseFacebookAds573A2ConverterPatch = bytecodePatch(
    name = "[Diag A2] Facebook 573 ads - CSR converter",
    description = "DEV diagnostic: only nulls the bZU Feed CSR response converter.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagFeedAdsResponseConverter.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val diagnoseFacebookAds573A3AsyncControllerPatch = bytecodePatch(
    name = "[Diag A3] Facebook 573 ads - Async controller",
    description = "DEV diagnostic: only replaces FeedAsyncAdsController output with an empty C6Ke.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagAsyncFeedAdsController.method.addInstructions(
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
    }
}

@Suppress("unused")
val diagnoseFacebookAds573BFinalFeedPatch = bytecodePatch(
    name = "[Diag B] Facebook 573 ads - Final feed filter",
    description = "DEV diagnostic: only filters SPONSORED/PROMOTION feed edges and MultiAds sponsored data.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagFeedEdgeInsertion.method.addInstructions(
            0,
            """
                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->B6k()Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                move-result-object v1
                sget-object v2, Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;->A0K:Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                if-eq v1, v2, :froggo_diag_ads573_drop_feed_edge
                sget-object v2, Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;->A0I:Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                if-eq v1, v2, :froggo_diag_ads573_drop_feed_edge
                goto :froggo_diag_ads573_keep_feed_edge

                :froggo_diag_ads573_drop_feed_edge
                const/4 v0, 0x0
                return v0

                :froggo_diag_ads573_keep_feed_edge
            """.trimIndent(),
        )
        diagMultiAdsSponsoredData.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val diagnoseFacebookAds573CVideoReelsPatch = bytecodePatch(
    name = "[Diag C] Facebook 573 ads - Reels/video",
    description = "DEV diagnostic: only disables Reels/video and commercial-break ad fetch/success paths.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        diagVideoAdBreakFetch.method.addInstructions(0, "return-void")
        diagVideoAdBreakSuccess.method.addInstructions(0, "return-void")
        diagReelsVideoAdFetch.method.addInstructions(0, "return-void")
        diagReelsBannerAdSuccess.method.addInstructions(0, "return-void")
        diagReelsVideoAdSuccess.method.addInstructions(0, "return-void")
    }
}

@Suppress("unused")
val diagnoseFacebookAds573DStoryPublicationPatch = bytecodePatch(
    name = "[Diag D] Facebook 573 Stories - publication lifecycle",
    description = "DEV diagnostic: logs Story bucket publication, cached replay and viewer notification without changing behavior.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        val fragmentInstructions = diagStoryFragmentCreate.method.implementation!!.instructions
        val subscriberSetIndex = fragmentInstructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.IPUT_OBJECT) return@indexOfFirst false
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "LX/AkQ;" && reference.name == "A05"
        }
        require(subscriberSetIndex >= 0) { "Could not find Story AkQ subscriber assignment" }
        val subscriberSetInstruction = fragmentInstructions[subscriberSetIndex] as? TwoRegisterInstruction
            ?: error("Unexpected Story AkQ subscriber assignment instruction")
        val subscriberControllerRegister = subscriberSetInstruction.registerB

        val publishRunnableInstructions = diagStoryPublishRunnable.method.implementation!!.instructions
        val subscriberReadIndices = publishRunnableInstructions.withIndex().mapNotNull { (index, instruction) ->
            if (instruction.opcode != Opcode.IGET_OBJECT) return@mapNotNull null
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            if (reference?.definingClass == "LX/AkQ;" && reference.name == "A05") index else null
        }
        require(subscriberReadIndices.size >= 2) { "Could not find both Story AkQ subscriber reads in Am0.run" }
        val subscriberReadIndex = subscriberReadIndices.first()
        val subscriberNullPathIndex = subscriberReadIndex + 2
        require(publishRunnableInstructions[subscriberReadIndex + 1].opcode == Opcode.IF_NEZ) {
            "Unexpected Story AkQ subscriber branch in Am0.run"
        }
        val compareA02ReadIndex = publishRunnableInstructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.IGET_OBJECT) return@indexOfFirst false
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "LX/AkQ;" && reference.name == "A02"
        }
        require(compareA02ReadIndex >= 0) { "Could not find Story AkQ A02 comparison read in Am0.run" }
        require(publishRunnableInstructions[compareA02ReadIndex + 1].opcode == Opcode.INVOKE_STATIC) {
            "Unexpected Story AkQ equality opcode"
        }
        val equalityResult = publishRunnableInstructions[compareA02ReadIndex + 2] as? OneRegisterInstruction
            ?: error("Unexpected Story AkQ equality result instruction")
        require(publishRunnableInstructions[compareA02ReadIndex + 2].opcode == Opcode.MOVE_RESULT) {
            "Unexpected Story AkQ equality result opcode"
        }
        val equalityResultRegister = equalityResult.registerA
        val equalityBranchIndex = compareA02ReadIndex + 3
        require(publishRunnableInstructions[equalityBranchIndex].opcode == Opcode.IF_NEZ) {
            "Unexpected Story AkQ equality branch opcode"
        }

        fun findMethodCallAfter(startIndex: Int, definingClass: String, methodName: String): Int =
            publishRunnableInstructions.withIndex().firstOrNull { (index, instruction) ->
                if (index <= startIndex || instruction !is ReferenceInstruction) return@firstOrNull false
                val reference = instruction.reference as? MethodReference ?: return@firstOrNull false
                reference.definingClass == definingClass && reference.name == methodName
            }?.index ?: error("Could not find $definingClass->$methodName after instruction $startIndex")

        val traceCallIndex = findMethodCallAfter(equalityBranchIndex, "LX/0D7;", "A03")
        val tryEntryIndex = publishRunnableInstructions.withIndex().firstOrNull { (index, instruction) ->
            if (index <= traceCallIndex || instruction.opcode != Opcode.IGET_OBJECT) return@firstOrNull false
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "LX/AkQ;" && reference.name == "A0S"
        }?.index ?: error("Could not find Am0 try-entry launch config read")
        val bucketLookupIndex = findMethodCallAfter(tryEntryIndex, "LX/Bsm;", "B3w")
        require(publishRunnableInstructions[bucketLookupIndex + 1].opcode == Opcode.MOVE_RESULT_OBJECT) {
            "Unexpected Story bucket lookup result sequence"
        }
        val flowAnnotateIndex = publishRunnableInstructions.withIndex().firstOrNull { (index, instruction) ->
            if (index <= bucketLookupIndex || instruction !is ReferenceInstruction) return@firstOrNull false
            val reference = instruction.reference as? MethodReference ?: return@firstOrNull false
            reference.name == "flowAnnotate"
        }?.index ?: error("Could not find Am0 flowAnnotate before viewer notification")
        val notifyCallIndex = findMethodCallAfter(flowAnnotateIndex, "LX/Al1;", "A00")
        val catchMoveExceptionIndex = publishRunnableInstructions.withIndex().lastOrNull { (index, instruction) ->
            index > notifyCallIndex && instruction.opcode == Opcode.MOVE_EXCEPTION
        }?.index ?: error("Could not find Am0 catchall move-exception")
        val catchThrowableRegister = (publishRunnableInstructions[catchMoveExceptionIndex] as? OneRegisterInstruction)?.registerA
            ?: error("Unexpected Am0 move-exception instruction")
        val storyNotifyInstructions = diagStoryNotify.method.implementation!!.instructions
        val al1StateStoreIndex = storyNotifyInstructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.IPUT_OBJECT) return@indexOfFirst false
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "LX/Aky;" && reference.name == "A0F"
        }
        require(al1StateStoreIndex >= 0) { "Could not find Story Aky.A0F state store in Al1.A00" }

        val al1ViewReadIndex = storyNotifyInstructions.withIndex().firstOrNull { (index, instruction) ->
            if (index <= al1StateStoreIndex || instruction.opcode != Opcode.IGET_OBJECT) return@firstOrNull false
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Landroidx/fragment/app/Fragment;" && reference.name == "mView"
        }?.index ?: error("Could not find Story Fragment.mView read in Al1.A00")
        val al1ViewRead = storyNotifyInstructions[al1ViewReadIndex] as? TwoRegisterInstruction
            ?: error("Unexpected Story Fragment.mView read in Al1.A00")
        val al1ViewRegister = al1ViewRead.registerA

        fun findStoryNotifyMethodCallAfter(startIndex: Int, definingClass: String, methodName: String): Int =
            storyNotifyInstructions.withIndex().firstOrNull { (index, instruction) ->
                if (index <= startIndex || instruction !is ReferenceInstruction) return@firstOrNull false
                val reference = instruction.reference as? MethodReference ?: return@firstOrNull false
                reference.definingClass == definingClass && reference.name == methodName
            }?.index ?: error("Could not find $definingClass->$methodName after instruction $startIndex in Al1.A00")

        val al1StateRefreshIndex = findStoryNotifyMethodCallAfter(al1ViewReadIndex, "LX/Aky;", "A0I")
        val al1InitializedReadIndex = storyNotifyInstructions.withIndex().firstOrNull { (index, instruction) ->
            if (index <= al1StateRefreshIndex || instruction.opcode != Opcode.IGET_BOOLEAN) return@firstOrNull false
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "LX/Aky;" && reference.name == "A0w"
        }?.index ?: error("Could not find Story Aky.A0w read in Al1.A00")
        val al1InitializedRead = storyNotifyInstructions[al1InitializedReadIndex] as? TwoRegisterInstruction
            ?: error("Unexpected Story Aky.A0w read in Al1.A00")
        val al1InitializedRegister = al1InitializedRead.registerA

        val al1InitialIndexCallIndex = findStoryNotifyMethodCallAfter(al1InitializedReadIndex, "LX/Alz;", "A00")
        require(storyNotifyInstructions[al1InitialIndexCallIndex + 1].opcode == Opcode.MOVE_RESULT) {
            "Unexpected Story initial-index result sequence in Al1.A00"
        }
        val al1InitialIndexResult = storyNotifyInstructions[al1InitialIndexCallIndex + 1] as? OneRegisterInstruction
            ?: error("Unexpected Story initial-index result instruction in Al1.A00")
        val al1InitialIndexRegister = al1InitialIndexResult.registerA

        val al1PagerUpdateCallIndex = findStoryNotifyMethodCallAfter(al1InitializedReadIndex, "LX/Al4;", "A07")
        val al1A06CallIndices = storyNotifyInstructions.withIndex().mapNotNull { (index, instruction) ->
            if (instruction !is ReferenceInstruction) return@mapNotNull null
            val reference = instruction.reference as? MethodReference ?: return@mapNotNull null
            if (reference.definingClass == "LX/Aky;" && reference.name == "A06") index else null
        }
        require(al1A06CallIndices.size == 2) { "Expected exactly two Story Aky.A06 calls in Al1.A00" }
        val al1PrimaryA06CallIndex = al1A06CallIndices.first()
        val al1FallbackA06CallIndex = al1A06CallIndices.last()

        val al1InitializedGuardCompareIndex = storyNotifyInstructions.withIndex().firstOrNull { (index, instruction) ->
            if (index <= al1InitializedReadIndex || index >= al1PagerUpdateCallIndex || instruction !is ReferenceInstruction) {
                return@firstOrNull false
            }
            val reference = instruction.reference as? MethodReference ?: return@firstOrNull false
            reference.definingClass == "Ljava/lang/Integer;" && reference.name == "compareTo"
        }?.index ?: error("Could not find initialized Story early-return guard in Al1.A00")
        require(storyNotifyInstructions[al1InitializedGuardCompareIndex + 1].opcode == Opcode.MOVE_RESULT) {
            "Unexpected initialized Story guard result in Al1.A00"
        }
        require(storyNotifyInstructions[al1InitializedGuardCompareIndex + 2].opcode == Opcode.IF_LTZ) {
            "Unexpected initialized Story guard branch in Al1.A00"
        }
        require(storyNotifyInstructions[al1InitializedGuardCompareIndex + 3].opcode == Opcode.RETURN_VOID) {
            "Unexpected initialized Story guard return in Al1.A00"
        }
        val al1InitializedGuardReturnIndex = al1InitializedGuardCompareIndex + 3

        val targetClass = diagStoryPublish.classDef
        val targetClassType = targetClass.type

        fun addMarkerHelper(methodName: String, message: String) {
            val helper = ImmutableMethod(
                targetClassType,
                methodName,
                emptyList(),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(2),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        const-string v0, "FroggoStoryDiag"
                        const-string v1, "$message"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        return-void
                    """.trimIndent(),
                )
            }
            targetClass.methods.add(helper)
        }

        val subscriberSetHelper = "froggoStoryDiagSubscriberSet"
        val subscriberNullHelper = "froggoStoryDiagSubscriberNull"
        val equalityResultHelper = "froggoStoryDiagEqualityResult"
        val beforeNotifyHelper = "froggoStoryDiagBeforeNotify"
        val afterNotifyCallHelper = "froggoStoryDiagAfterNotifyCall"
        val throwableHelper = "froggoStoryDiagThrowable"
        val al1EnterHelper = "froggoStoryDiagAl1Enter"
        val al1AfterStateStoreHelper = "froggoStoryDiagAl1AfterStateStore"
        val al1ViewStateHelper = "froggoStoryDiagAl1ViewState"
        val al1InitializedStateHelper = "froggoStoryDiagAl1InitializedState"
        val al1InitialIndexHelper = "froggoStoryDiagAl1InitialIndex"
        val al1BeforePagerUpdateHelper = "froggoStoryDiagAl1BeforePagerUpdate"
        val al1AfterPagerUpdateHelper = "froggoStoryDiagAl1AfterPagerUpdate"
        val al1InitializedGuardReturnHelper = "froggoStoryDiagAl1InitializedGuardReturn"
        val al1BeforePrimaryA06Helper = "froggoStoryDiagAl1BeforePrimaryA06"
        val al1AfterPrimaryA06Helper = "froggoStoryDiagAl1AfterPrimaryA06"
        val al1BeforeFallbackA06Helper = "froggoStoryDiagAl1BeforeFallbackA06"
        val al1AfterFallbackA06Helper = "froggoStoryDiagAl1AfterFallbackA06"
        addMarkerHelper(subscriberNullHelper, "AM0_SUBSCRIBER_NULL")
        addMarkerHelper(beforeNotifyHelper, "AM0_BEFORE_NOTIFY")
        addMarkerHelper(afterNotifyCallHelper, "AM0_AFTER_NOTIFY_CALL")
        addMarkerHelper(al1EnterHelper, "AL1_ENTER")
        addMarkerHelper(al1AfterStateStoreHelper, "AL1_AFTER_AKY_STATE_STORE")
        addMarkerHelper(al1BeforePagerUpdateHelper, "AL1_BEFORE_PAGER_UPDATE")
        addMarkerHelper(al1AfterPagerUpdateHelper, "AL1_AFTER_PAGER_UPDATE")
        addMarkerHelper(al1InitializedGuardReturnHelper, "AL1_INITIALIZED_GUARD_RETURN")
        addMarkerHelper(al1BeforePrimaryA06Helper, "AL1_BEFORE_AKY_A06_PRIMARY")
        addMarkerHelper(al1AfterPrimaryA06Helper, "AL1_AFTER_AKY_A06_PRIMARY")
        addMarkerHelper(al1BeforeFallbackA06Helper, "AL1_BEFORE_AKY_A06_FALLBACK")
        addMarkerHelper(al1AfterFallbackA06Helper, "AL1_AFTER_AKY_A06_FALLBACK")

        targetClass.methods.add(
            ImmutableMethod(
                targetClassType,
                throwableHelper,
                listOf(ImmutableMethodParameter("Ljava/lang/Throwable;", null, null)),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        const-string v0, "FroggoStoryDiag"
                        const-string v1, "AM0_THROW"
                        invoke-static {v0, v1, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
                        return-void
                    """.trimIndent(),
                )
            },
        )

        targetClass.methods.add(
            ImmutableMethod(
                targetClassType,
                al1ViewStateHelper,
                listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        if-nez p0, :froggo_diag_al1_view_nonnull
                        const-string v1, "AL1_VIEW_NULL"
                        goto :froggo_diag_al1_log_view
                        :froggo_diag_al1_view_nonnull
                        const-string v1, "AL1_VIEW_NONNULL"
                        :froggo_diag_al1_log_view
                        const-string v0, "FroggoStoryDiag"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        return-void
                    """.trimIndent(),
                )
            },
        )

        targetClass.methods.add(
            ImmutableMethod(
                targetClassType,
                al1InitializedStateHelper,
                listOf(ImmutableMethodParameter("Z", null, null)),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        if-eqz p0, :froggo_diag_al1_initializing
                        const-string v1, "AL1_ALREADY_INITIALIZED"
                        goto :froggo_diag_al1_log_initialized
                        :froggo_diag_al1_initializing
                        const-string v1, "AL1_INITIALIZING"
                        :froggo_diag_al1_log_initialized
                        const-string v0, "FroggoStoryDiag"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        return-void
                    """.trimIndent(),
                )
            },
        )

        targetClass.methods.add(
            ImmutableMethod(
                targetClassType,
                al1InitialIndexHelper,
                listOf(ImmutableMethodParameter("I", null, null)),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        const-string v0, "FroggoStoryDiag"
                        const-string v1, "AL1_INITIAL_INDEX"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        invoke-static {p0}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
                        move-result-object v1
                        const-string v0, "FroggoStoryDiagInitialIndex"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        return-void
                    """.trimIndent(),
                )
            },
        )

        targetClass.methods.add(
            ImmutableMethod(
                targetClassType,
                subscriberSetHelper,
                listOf(ImmutableMethodParameter(targetClassType, null, null)),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(4),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        const-string v0, "FroggoStoryDiag"
                        const-string v1, "SUBSCRIBER_SET"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
                        move-result v1
                        invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
                        move-result-object v1
                        const-string v0, "FroggoStoryDiagFragmentAkQId"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        return-void
                    """.trimIndent(),
                )
            },
        )

        targetClass.methods.add(
            ImmutableMethod(
                targetClassType,
                equalityResultHelper,
                listOf(ImmutableMethodParameter("Z", null, null)),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        if-eqz p0, :froggo_diag_equal_false
                        const-string v1, "CMP_EQUAL_TRUE"
                        goto :froggo_diag_log_equal
                        :froggo_diag_equal_false
                        const-string v1, "CMP_EQUAL_FALSE"
                        :froggo_diag_log_equal
                        const-string v0, "FroggoStoryDiag"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        return-void
                    """.trimIndent(),
                )
            },
        )

        // Instrument Al1 from higher offsets to lower offsets so every index still
        // refers to the stock method. Entry is deliberately placed after the first
        // stock instruction and all branch-state probes are helper calls, matching
        // the marker mechanism that is already reliable in Am0.
        diagStoryNotify.method.addInstructions(
            al1FallbackA06CallIndex + 1,
            "invoke-static {}, $targetClassType->$al1AfterFallbackA06Helper()V",
        )
        diagStoryNotify.method.addInstructions(
            al1FallbackA06CallIndex,
            "invoke-static {}, $targetClassType->$al1BeforeFallbackA06Helper()V",
        )
        diagStoryNotify.method.addInstructions(
            al1PrimaryA06CallIndex + 1,
            "invoke-static {}, $targetClassType->$al1AfterPrimaryA06Helper()V",
        )
        diagStoryNotify.method.addInstructions(
            al1PrimaryA06CallIndex,
            "invoke-static {}, $targetClassType->$al1BeforePrimaryA06Helper()V",
        )
        diagStoryNotify.method.addInstructions(
            al1InitialIndexCallIndex + 2,
            "invoke-static/range {v$al1InitialIndexRegister .. v$al1InitialIndexRegister}, $targetClassType->$al1InitialIndexHelper(I)V",
        )
        diagStoryNotify.method.addInstructions(
            al1PagerUpdateCallIndex + 1,
            "invoke-static {}, $targetClassType->$al1AfterPagerUpdateHelper()V",
        )
        diagStoryNotify.method.addInstructions(
            al1PagerUpdateCallIndex,
            "invoke-static {}, $targetClassType->$al1BeforePagerUpdateHelper()V",
        )
        diagStoryNotify.method.addInstructions(
            al1InitializedGuardReturnIndex,
            "invoke-static {}, $targetClassType->$al1InitializedGuardReturnHelper()V",
        )
        diagStoryNotify.method.addInstructions(
            al1InitializedReadIndex + 1,
            "invoke-static/range {v$al1InitializedRegister .. v$al1InitializedRegister}, $targetClassType->$al1InitializedStateHelper(Z)V",
        )
        diagStoryNotify.method.addInstructions(
            al1ViewReadIndex + 1,
            "invoke-static/range {v$al1ViewRegister .. v$al1ViewRegister}, $targetClassType->$al1ViewStateHelper(Ljava/lang/Object;)V",
        )
        diagStoryNotify.method.addInstructions(
            al1StateStoreIndex + 1,
            "invoke-static {}, $targetClassType->$al1AfterStateStoreHelper()V",
        )
        diagStoryNotify.method.addInstructions(
            1,
            "invoke-static {}, $targetClassType->$al1EnterHelper()V",
        )

        // Insert higher offsets first so each index still refers to the stock method.
        // Every new flow marker is placed *after* a stock instruction rather than
        // before a branch target. This prevents existing labels from jumping over
        // the diagnostic call.
        diagStoryPublishRunnable.method.addInstructions(
            catchMoveExceptionIndex + 1,
            "invoke-static {v$catchThrowableRegister}, $targetClassType->$throwableHelper(Ljava/lang/Throwable;)V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            notifyCallIndex + 1,
            "invoke-static {}, $targetClassType->$afterNotifyCallHelper()V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            flowAnnotateIndex + 1,
            "invoke-static {}, $targetClassType->$beforeNotifyHelper()V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            compareA02ReadIndex + 3,
            "invoke-static {v$equalityResultRegister}, $targetClassType->$equalityResultHelper(Z)V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            subscriberNullPathIndex,
            "invoke-static {}, $targetClassType->$subscriberNullHelper()V",
        )
        diagStoryFragmentCreate.method.addInstructions(
            subscriberSetIndex + 1,
            "invoke-static/range {v$subscriberControllerRegister .. v$subscriberControllerRegister}, $targetClassType->$subscriberSetHelper($targetClassType)V",
        )

        diagStoryFragmentCreate.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "FRAGMENT_CREATE"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
        diagStoryPublish.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "AKQ_A02_PUBLISH"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
        diagStoryPublishRunnable.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "AM0_RUN"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
        diagStoryCachedReplay.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "AKQ_REPLAY_CACHED"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
    }
}
