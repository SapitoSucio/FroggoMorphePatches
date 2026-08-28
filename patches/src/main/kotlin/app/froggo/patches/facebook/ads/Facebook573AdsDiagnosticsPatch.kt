package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
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
        val entrySubscriberRead = publishRunnableInstructions[subscriberReadIndex] as? TwoRegisterInstruction
            ?: error("Unexpected Story AkQ entry subscriber read")
        val entrySubscriberRegister = entrySubscriberRead.registerA
        val entryControllerRegister = entrySubscriberRead.registerB

        val compareA02ReadIndex = publishRunnableInstructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.IGET_OBJECT) return@indexOfFirst false
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "LX/AkQ;" && reference.name == "A02"
        }
        require(compareA02ReadIndex >= 0) { "Could not find Story AkQ A02 comparison read in Am0.run" }
        val compareA02Read = publishRunnableInstructions[compareA02ReadIndex] as? TwoRegisterInstruction
            ?: error("Unexpected Story AkQ A02 comparison instruction")
        val compareControllerRegister = compareA02Read.registerB
        val equalityCall = publishRunnableInstructions[compareA02ReadIndex + 1] as? FiveRegisterInstruction
            ?: error("Unexpected Story AkQ equality call")
        require(publishRunnableInstructions[compareA02ReadIndex + 1].opcode == Opcode.INVOKE_STATIC) {
            "Unexpected Story AkQ equality opcode"
        }
        val selectedSnapshotRegister = equalityCall.registerD
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
        val notifySubscriberReadIndex = subscriberReadIndices.lastOrNull { it in (flowAnnotateIndex + 1) until notifyCallIndex }
            ?: error("Could not find Story AkQ subscriber re-read before Al1.A00")
        val notifySubscriberRead = publishRunnableInstructions[notifySubscriberReadIndex] as? TwoRegisterInstruction
            ?: error("Unexpected Story AkQ notify subscriber read")
        val notifySubscriberRegister = notifySubscriberRead.registerA
        val notifyControllerRegister = notifySubscriberRead.registerB

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
        val compareStateHelper = "froggoStoryDiagCompareState"
        val equalityResultHelper = "froggoStoryDiagEqualityResult"
        val afterEqualityBranchHelper = "froggoStoryDiagAfterEqualityBranch"
        val afterTraceHelper = "froggoStoryDiagAfterTrace"
        val enteredTryHelper = "froggoStoryDiagEnteredTry"
        val afterBucketLookupHelper = "froggoStoryDiagAfterBucketLookup"
        val beforeNotifyHelper = "froggoStoryDiagBeforeNotify"
        val afterNotifyCallHelper = "froggoStoryDiagAfterNotifyCall"
        val throwableHelper = "froggoStoryDiagThrowable"
        val entrySubscriberStateHelper = "froggoStoryDiagEntrySubscriberState"
        val notifySubscriberStateHelper = "froggoStoryDiagNotifySubscriberState"
        addMarkerHelper(subscriberNullHelper, "AM0_SUBSCRIBER_NULL")
        addMarkerHelper(afterEqualityBranchHelper, "AM0_AFTER_EQUALITY_BRANCH")
        addMarkerHelper(afterTraceHelper, "AM0_AFTER_TRACE")
        addMarkerHelper(enteredTryHelper, "AM0_ENTERED_TRY")
        addMarkerHelper(afterBucketLookupHelper, "AM0_AFTER_BUCKET_LOOKUP")
        addMarkerHelper(beforeNotifyHelper, "AM0_BEFORE_NOTIFY")
        addMarkerHelper(afterNotifyCallHelper, "AM0_AFTER_NOTIFY_CALL")

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

        fun addSubscriberStateHelper(
            methodName: String,
            nullMarker: String,
            nonNullMarker: String,
            controllerTag: String,
            subscriberTag: String,
        ) {
            targetClass.methods.add(
                ImmutableMethod(
                    targetClassType,
                    methodName,
                    listOf(
                        ImmutableMethodParameter(targetClassType, null, null),
                        ImmutableMethodParameter("LX/Al1;", null, null),
                    ),
                    "V",
                    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                    null,
                    null,
                    MutableMethodImplementation(5),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                            invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
                            move-result v1
                            invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
                            move-result-object v1
                            const-string v0, "$controllerTag"
                            invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

                            if-nez p1, :froggo_diag_subscriber_nonnull
                            const-string v1, "$nullMarker"
                            const-string v0, "FroggoStoryDiag"
                            invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                            return-void

                            :froggo_diag_subscriber_nonnull
                            const-string v1, "$nonNullMarker"
                            const-string v0, "FroggoStoryDiag"
                            invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                            invoke-static {p1}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
                            move-result v1
                            invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
                            move-result-object v1
                            const-string v0, "$subscriberTag"
                            invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                            return-void
                        """.trimIndent(),
                    )
                },
            )
        }

        addSubscriberStateHelper(
            entrySubscriberStateHelper,
            "AM0_ENTRY_A05_NULL",
            "AM0_ENTRY_A05_NONNULL",
            "FroggoStoryDiagEntryAkQId",
            "FroggoStoryDiagEntrySubId",
        )
        addSubscriberStateHelper(
            notifySubscriberStateHelper,
            "AM0_NOTIFY_A05_NULL",
            "AM0_NOTIFY_A05_NONNULL",
            "FroggoStoryDiagNotifyId",
            "FroggoStoryDiagNotifySubId",
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
                        const-string v0, "FroggoStoryDiagSubId"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                        return-void
                    """.trimIndent(),
                )
            },
        )

        targetClass.methods.add(
            ImmutableMethod(
                targetClassType,
                compareStateHelper,
                listOf(
                    ImmutableMethodParameter(targetClassType, null, null),
                    ImmutableMethodParameter("LX/Bsm;", null, null),
                ),
                "V",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(6),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I
                        move-result v1
                        invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
                        move-result-object v1
                        const-string v0, "FroggoStoryDiagAm0Id"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

                        iget-object v2, p0, LX/AkQ;->A02:LX/Bsm;
                        if-nez v2, :froggo_diag_a02_nonnull
                        const-string v1, "CMP_A02_NULL"
                        goto :froggo_diag_log_a02
                        :froggo_diag_a02_nonnull
                        const-string v1, "CMP_A02_NONNULL"
                        :froggo_diag_log_a02
                        const-string v0, "FroggoStoryDiag"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

                        if-nez p1, :froggo_diag_selected_nonnull
                        const-string v1, "CMP_SELECTED_NULL"
                        goto :froggo_diag_log_selected
                        :froggo_diag_selected_nonnull
                        const-string v1, "CMP_SELECTED_NONNULL"
                        :froggo_diag_log_selected
                        const-string v0, "FroggoStoryDiag"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

                        iget-object v2, p0, LX/AkQ;->A0Z:Ljava/util/concurrent/atomic/AtomicReference;
                        invoke-virtual {v2}, Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;
                        move-result-object v2
                        if-nez v2, :froggo_diag_a0z_nonnull
                        const-string v1, "CMP_A0Z_NULL"
                        goto :froggo_diag_log_a0z
                        :froggo_diag_a0z_nonnull
                        const-string v1, "CMP_A0Z_NONNULL"
                        :froggo_diag_log_a0z
                        const-string v0, "FroggoStoryDiag"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

                        if-ne v2, p1, :froggo_diag_a0z_diff
                        const-string v1, "CMP_A0Z_SAME_SELECTED"
                        goto :froggo_diag_log_a0z_identity
                        :froggo_diag_a0z_diff
                        const-string v1, "CMP_A0Z_DIFF_SELECTED"
                        :froggo_diag_log_a0z_identity
                        const-string v0, "FroggoStoryDiag"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

                        iget-object v2, p0, LX/AkQ;->A02:LX/Bsm;
                        if-ne v2, p1, :froggo_diag_a02_diff
                        const-string v1, "CMP_A02_SAME_SELECTED"
                        goto :froggo_diag_log_a02_identity
                        :froggo_diag_a02_diff
                        const-string v1, "CMP_A02_DIFF_SELECTED"
                        :froggo_diag_log_a02_identity
                        const-string v0, "FroggoStoryDiag"
                        invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

                        iget-object v2, p0, LX/AkQ;->A0Y:Ljava/util/concurrent/atomic/AtomicBoolean;
                        invoke-virtual {v2}, Ljava/util/concurrent/atomic/AtomicBoolean;->get()Z
                        move-result v2
                        if-eqz v2, :froggo_diag_a0y_false
                        const-string v1, "CMP_A0Y_TRUE"
                        goto :froggo_diag_log_a0y
                        :froggo_diag_a0y_false
                        const-string v1, "CMP_A0Y_FALSE"
                        :froggo_diag_log_a0y
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
            notifySubscriberReadIndex + 1,
            "invoke-static {v$notifyControllerRegister, v$notifySubscriberRegister}, $targetClassType->$notifySubscriberStateHelper(${targetClassType}LX/Al1;)V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            flowAnnotateIndex + 1,
            "invoke-static {}, $targetClassType->$beforeNotifyHelper()V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            bucketLookupIndex + 2,
            "invoke-static {}, $targetClassType->$afterBucketLookupHelper()V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            tryEntryIndex + 1,
            "invoke-static {}, $targetClassType->$enteredTryHelper()V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            traceCallIndex + 1,
            "invoke-static {}, $targetClassType->$afterTraceHelper()V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            equalityBranchIndex + 1,
            "invoke-static {}, $targetClassType->$afterEqualityBranchHelper()V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            compareA02ReadIndex + 3,
            "invoke-static {v$equalityResultRegister}, $targetClassType->$equalityResultHelper(Z)V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            subscriberNullPathIndex,
            "invoke-static {}, $targetClassType->$subscriberNullHelper()V",
        )
        diagStoryPublishRunnable.method.addInstructions(
            subscriberReadIndex + 1,
            "invoke-static {v$entryControllerRegister, v$entrySubscriberRegister}, $targetClassType->$entrySubscriberStateHelper(${targetClassType}LX/Al1;)V",
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
        diagStoryNotify.method.addInstructions(
            0,
            """
                const-string v0, "FroggoStoryDiag"
                const-string v1, "AL1_NOTIFY"
                invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
                move-result v0
            """.trimIndent(),
        )
    }
}
