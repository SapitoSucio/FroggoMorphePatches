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
 * Keep X.2UL completely stock. Neutralize only automatic lifecycle decisions:
 * foreground revisit, onResume revisit, hot-start/tab visibility refresh, the
 * onPause stale-post worker, the friendly-feed prefetch on app entry, and the
 * stale NewsFeedTabDataFetch AUTO_REFRESH.
 * Activity-result, fullscreen close, cold initialization and manual refresh remain
 * untouched.
 */
package app.froggo.patches.facebook.refresh

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
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

private val newsFeedTabDataFetchDispatch = Fingerprint(
    returnType = "V",
    parameters = listOf("LX/4RQ;", "LX/cbp;"),
    custom = { method, classDef ->
        classDef.type == "LX/4RQ;" && method.name == "A00"
    },
)

private val newsFeedVisibilityChanged = Fingerprint(
    returnType = "V",
    parameters = listOf("Z", "Z"),
    custom = { method, classDef ->
        classDef.type == "LX/2Q7;" && method.name == "onSetUserVisibleHint"
    },
)

private val newsFeedOnPauseStaleRefresh = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        classDef.type == "LX/6Tk;" && method.name == "run"
    },
)

private val newsFeedOnAppExit = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        classDef.type == "LX/2Q7;" && method.name == "A0m"
    },
)

private val mainFeedNetworkResponse = Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/google/common/collect/ImmutableList;", "LX/1bF;"),
    custom = { method, classDef ->
        classDef.type == "LX/1wV;" && method.name == "CNG"
    },
)

private val mainFeedHeadLoad = Fingerprint(
    returnType = "I",
    parameters = listOf("LX/1an;", "Ljava/lang/String;"),
    custom = { method, classDef ->
        classDef.type == "LX/1wV;" && method.name == "A0M"
    },
)

@Suppress("unused")
val blockFacebookAutomaticRefresh573Patch = bytecodePatch(
    name = "Block Facebook automatic refresh (573)",
    description = "Experimental: blocks automatic foreground/hot-start/stale-tab/stale-post feed refresh while preserving cold initialization, manual, activity-result and fullscreen refresh paths.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL)

    execute {
        // MainFeedCSRDataLoaderImpl.A0M mutates the cursor before delegating to
        // the generic loader. Drop only automatic causes after initialization so
        // that a background/foreground revisit cannot clear or rebuild the feed.
        // INITIAL, manual, pull-to-refresh, tab-click and network-error causes
        // retain the stock path. FRIENDLY_FEED_PREFETCH is also dropped only
        // after initialization; cold initialization must remain stock.
        mainFeedHeadLoad.method.addInstructions(
            0,
            """
                move-object v1, p1
                invoke-virtual {v1}, LX/1an;->A00()Z
                move-result v0
                if-nez v0, :froggo_refresh573_check_initialized_head_load
                sget-object v0, LX/1an;->A0d:LX/1an;
                if-eq v1, v0, :froggo_refresh573_check_initialized_head_load
                sget-object v0, LX/1an;->A0E:LX/1an;
                if-ne v1, v0, :froggo_refresh573_keep_head_load
                :froggo_refresh573_check_initialized_head_load
                invoke-virtual {p0}, LX/cbg;->A0R()LX/1l5;
                move-result-object v0
                sget-object v1, LX/1l5;->INITIAL:LX/1l5;
                if-eq v0, v1, :froggo_refresh573_keep_head_load
                const/4 v0, 0x0
                return v0

                :froggo_refresh573_keep_head_load
            """.trimIndent(),
        )

        val appExitInstructions = newsFeedOnAppExit.method.implementation!!.instructions
        val appExitTeardownCalls = appExitInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                reference?.definingClass == "LX/cbp;" &&
                    reference.name == "A0Q" &&
                    reference.parameterTypes == listOf("Z")
            ) {
                index
            } else {
                null
            }
        }
        require(appExitTeardownCalls.size == 1) {
            "Expected exactly one delayed Main Feed teardown call on app exit"
        }

        // JADX shows that C2Q7.A0m is the app-state exit callback. Its final
        // A0Q() schedules the delayed loader teardown; after that teardown the
        // next foreground is a new INITIAL load and replaces the visible feed.
        // Skip only this automatic teardown call. Keep supplemental prefetch,
        // cold initialization and all manual refresh paths intact.
        newsFeedOnAppExit.method.addInstructions(
            appExitTeardownCalls.single(),
            "return-void",
        )

        // Automatic causes can still reach the MainFeed network-response boundary
        // after the lifecycle/tab callsite guards. Once the loader is initialized,
        // CNG passes that response to the CSR coordinator, which adds/re-vends it;
        // this is enough to re-materialize the visible Feed even when the pool size
        // does not change. Drop only the five automatic causes at this concrete main
        // Feed boundary. Initialization and manual/network-error paths are untouched.
        mainFeedNetworkResponse.method.addInstructions(
            0,
            """
                move-object/from16 v1, p2
                iget-object v0, v1, LX/1bF;->A03:LX/1bD;
                iget-object v0, v0, LX/1bD;->A02:LX/1an;
                move-object v2, v0
                invoke-virtual {v0}, LX/1an;->A00()Z
                move-result v0
                if-nez v0, :froggo_refresh573_check_initialized_network_response
                sget-object v0, LX/1an;->A0d:LX/1an;
                if-ne v2, v0, :froggo_refresh573_keep_network_response
                :froggo_refresh573_check_initialized_network_response
                move-object/from16 v1, p0
                invoke-virtual {v1}, LX/cbg;->A0R()LX/1l5;
                move-result-object v0
                sget-object v1, LX/1l5;->INITIAL:LX/1l5;
                if-eq v0, v1, :froggo_refresh573_keep_network_response
                :froggo_refresh573_drop_network_response
                return-void

                :froggo_refresh573_keep_network_response
            """.trimIndent(),
        )

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

        val tabDataFetchInstructions = newsFeedTabDataFetchDispatch.method.implementation!!.instructions
        val tabDataAutoRefreshCalls = tabDataFetchInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                reference?.definingClass == "LX/cbp;" &&
                reference.name == "A0M" &&
                reference.parameterTypes == listOf("LX/1cP;", "Ljava/lang/String;")
            ) {
                index
            } else {
                null
            }
        }
        require(tabDataAutoRefreshCalls.size == 1) {
            "Expected exactly one NewsFeedTabDataFetch AUTO_REFRESH dispatch"
        }
        val tabDataAutoRefreshCallIndex = tabDataAutoRefreshCalls.single()
        val tabDataAutoRefreshCall = tabDataFetchInstructions[tabDataAutoRefreshCallIndex] as? FiveRegisterInstruction
            ?: error("Expected fixed-register NewsFeedTabDataFetch AUTO_REFRESH invoke")
        require(
            tabDataAutoRefreshCall.registerCount == 3 &&
                tabDataAutoRefreshCall.registerC == 3 &&
                tabDataAutoRefreshCall.registerD == 0 &&
                tabDataAutoRefreshCall.registerE == 1,
        ) {
            "Unexpected NewsFeedTabDataFetch AUTO_REFRESH registers"
        }

        // NewsFeedTabDataFetch is mounted by NewsFeedFragment. Its delegate's A0A()
        // returns stale (3) after a MobileConfig TTL; the generic DataFetch framework
        // then calls emitter.Aok(2), which ultimately reaches this initialized-feed
        // branch and unconditionally forceRefresh(AUTO_REFRESH, "NewsFeedTabDataFetchSpec").
        //
        // Preserve the stock uninitialized branch (cold feed initialization). When
        // control reaches this callsite the data loader is already initialized, so
        // run the same common CqR callback that stock code executes after the refresh
        // and return without starting a new head load. No labels or large-method edits.
        newsFeedTabDataFetchDispatch.method.addInstructions(
            tabDataAutoRefreshCallIndex,
            """
                iget-object v1, p0, LX/4RQ;->A0A:LX/1rC;
                iget-object v0, p0, LX/4RQ;->A03:Lcom/facebook/api/feedtype/FeedType;
                invoke-virtual {v1, v0}, LX/1rC;->A01(Lcom/facebook/api/feedtype/FeedType;)LX/3yT;
                move-result-object v0
                invoke-interface {v0}, LX/3yT;->CqR()V
                return-void
            """.trimIndent(),
        )

        val visibilityInstructions = newsFeedVisibilityChanged.method.implementation!!.instructions
        val visibilityHotStartCalls = visibilityInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (reference?.definingClass == "LX/2UL;" && reference.name == "A0J") index else null
        }
        require(visibilityHotStartCalls.size == 1) {
            "Expected exactly one maybeRefreshForHotStart call in NewsFeedFragment.onSetUserVisibleHint"
        }
        val hotStartCallIndex = visibilityHotStartCalls.single()
        val hotStartGateIndex = hotStartCallIndex - 1
        val hotStartGate = visibilityInstructions[hotStartGateIndex] as? OneRegisterInstruction
            ?: error("Expected hot-start MobileConfig gate before NewsFeedFragment.onSetUserVisibleHint A0J")
        require(visibilityInstructions[hotStartGateIndex].opcode == Opcode.IF_EQZ && hotStartGate.registerA <= 15) {
            "Unexpected hot-start gate before NewsFeedFragment.onSetUserVisibleHint A0J"
        }

        val visibilityStaleRefreshCalls = visibilityInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (reference?.definingClass == "LX/2UL;" && reference.name == "A0A") index else null
        }
        require(visibilityStaleRefreshCalls.size == 1) {
            "Expected exactly one stale-post decision call in NewsFeedFragment.onSetUserVisibleHint"
        }
        val visibilityStaleRefreshCallIndex = visibilityStaleRefreshCalls.single()
        val visibilityStaleRefreshCall = visibilityInstructions[visibilityStaleRefreshCallIndex] as? FiveRegisterInstruction
            ?: error("Expected fixed-register stale-post call in NewsFeedFragment.onSetUserVisibleHint")
        require(
            visibilityStaleRefreshCall.registerCount == 3 &&
                visibilityStaleRefreshCall.registerC == 2 &&
                visibilityStaleRefreshCall.registerD <= 15,
        ) {
            "Unexpected stale-post decision registers in NewsFeedFragment.onSetUserVisibleHint"
        }

        // Force the hot-start MC branch onto the stale-post decision path, then
        // force that decision to 7 (Skip head load). This prevents both the direct
        // maybeRefreshForHotStart head load and C6RY rerank/network refresh actions.
        // Insert the later mutation first so the earlier index remains valid.
        newsFeedVisibilityChanged.method.addInstructions(
            visibilityStaleRefreshCallIndex,
            "const/4 v${visibilityStaleRefreshCall.registerD}, 0x7",
        )
        newsFeedVisibilityChanged.method.addInstructions(
            hotStartGateIndex,
            "const/4 v${hotStartGate.registerA}, 0x0",
        )

        val pauseInstructions = newsFeedOnPauseStaleRefresh.method.implementation!!.instructions
        val pauseStaleRefreshCalls = pauseInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (reference?.definingClass == "LX/2UL;" && reference.name == "A0A") index else null
        }
        require(pauseStaleRefreshCalls.size == 1) {
            "Expected exactly one stale-post decision call in the NewsFeed onPause worker"
        }
        val pauseStaleRefreshCallIndex = pauseStaleRefreshCalls.single()
        val pauseStaleRefreshCall = pauseInstructions[pauseStaleRefreshCallIndex] as? FiveRegisterInstruction
            ?: error("Expected fixed-register stale-post call in the NewsFeed onPause worker")
        require(pauseStaleRefreshCall.registerCount == 3 && pauseStaleRefreshCall.registerD <= 15) {
            "Unexpected stale-post decision registers in the NewsFeed onPause worker"
        }

        // The worker is explicitly named refresh_stale_post_on_pause. Keep the
        // worker/logging intact but execute decision 7, which performs no head load.
        newsFeedOnPauseStaleRefresh.method.addInstructions(
            pauseStaleRefreshCallIndex,
            "const/4 v${pauseStaleRefreshCall.registerD}, 0x7",
        )

    }
}
