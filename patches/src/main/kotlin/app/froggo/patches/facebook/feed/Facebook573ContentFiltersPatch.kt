package app.froggo.patches.facebook.feed

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private val feedEdgeInsertion = Fingerprint(
    parameters = listOf(
        "Lcom/google/common/collect/ImmutableList\$Builder;",
        "Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;",
        "LX/1cP;",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/1vv;" && method.name == "addNewEdgeToCollection"
    },
)

private val feedUnitActorClassifier = Fingerprint(
    parameters = listOf("LX/3yh;"),
    custom = { method, classDef ->
        classDef.type == "LX/3JE;" &&
            method.name == "A00" &&
            method.returnType == "Ljava/lang/String;"
    },
)

@Suppress("unused")
val hideFacebookAiContent573Patch = bytecodePatch(
    name = "Hide Facebook AI content (573)",
    description = "Filters Feed posts carrying Facebook's GenAI transparency metadata (Contenido de IA).",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        feedEdgeInsertion.method.addInstructions(
            0,
            """
                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->BO4()LX/3S1;
                move-result-object v0
                instance-of v1, v0, Lcom/facebook/graphql/model/GraphQLStory;
                if-eqz v1, :froggo_ai573_keep_edge
                check-cast v0, Lcom/facebook/graphql/model/GraphQLStory;
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLStory;->A0W()LX/41R;
                move-result-object v0
                if-eqz v0, :froggo_ai573_keep_edge
                const v1, 0x723ea5fe
                invoke-virtual {v0, v1}, LX/41R;->getCachedBoolean(I)Z
                move-result v0
                if-eqz v0, :froggo_ai573_keep_edge

                :froggo_ai573_hide_edge
                const/4 v0, 0x0
                return v0

                :froggo_ai573_keep_edge
                nop
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val facebook573AiContentDiagnosticsPatch = bytecodePatch(
    name = "Facebook 573 AI content diagnostics",
    description = "Logs Facebook GenAI structural flags for Feed stories without filtering them.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        feedEdgeInsertion.method.addInstructions(
            0,
            """
                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->BO4()LX/3S1;
                move-result-object v0
                instance-of v1, v0, Lcom/facebook/graphql/model/GraphQLStory;
                if-eqz v1, :froggo_ai_diag573_done
                check-cast v0, Lcom/facebook/graphql/model/GraphQLStory;
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLStory;->A0W()LX/41R;
                move-result-object v0
                if-eqz v0, :froggo_ai_diag573_check_self
                const v1, 0x723ea5fe
                invoke-virtual {v0, v1}, LX/41R;->getCachedBoolean(I)Z
                move-result v0
                if-eqz v0, :froggo_ai_diag573_detected_false
                const-string v0, "FroggoAiDiag573"
                const-string v1, "detectedInfo=1 detected=1"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                goto :froggo_ai_diag573_check_self

                :froggo_ai_diag573_detected_false
                const-string v0, "FroggoAiDiag573"
                const-string v1, "detectedInfo=1 detected=0"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                :froggo_ai_diag573_check_self
                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->BO4()LX/3S1;
                move-result-object v0
                check-cast v0, Lcom/facebook/graphql/model/GraphQLStory;
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLStory;->A0X()LX/41R;
                move-result-object v0
                if-eqz v0, :froggo_ai_diag573_done
                const v1, -0x439184bd
                invoke-virtual {v0, v1}, LX/41R;->getCachedBoolean(I)Z
                move-result v0
                if-eqz v0, :froggo_ai_diag573_self_false
                const-string v0, "FroggoAiDiag573"
                const-string v1, "selfDisclosureInfo=1 disclosed=1"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                goto :froggo_ai_diag573_done

                :froggo_ai_diag573_self_false
                const-string v0, "FroggoAiDiag573"
                const-string v1, "selfDisclosureInfo=1 disclosed=0"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                :froggo_ai_diag573_done
                nop
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val facebook573AiFilterSuggestedDiagnosticsPatch = bytecodePatch(
    name = "Facebook 573 AI filter + recommendation diagnostics",
    description = "Filters detected AI Feed stories and logs structural metadata for DiscoverFeedUnit recommendation candidates in one bytecode injection.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        feedUnitActorClassifier.method.addInstructions(
            0,
            """
                invoke-interface {p0}, LX/3yh;->BO4()LX/3S1;
                move-result-object v0
                if-eqz v0, :froggo_discover573_diag_done
                const v1, -0x91415ea
                invoke-static {v0, v1}, LX/41W;->A00(Ljava/lang/Object;I)Z
                move-result v1
                if-eqz v1, :froggo_discover573_diag_done

                const-string v1, "FroggoFeedDiag573"
                const-string v2, "candidate type=DiscoverFeedUnit"
                invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                check-cast v0, LX/41T;
                invoke-virtual {v0}, LX/41T;->BTc()Ljava/lang/String;
                move-result-object v1
                if-eqz v1, :froggo_discover573_no_hideable
                const-string v1, "FroggoFeedDiag573"
                const-string v2, "discover hideableTokenPresent=1"
                invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                goto :froggo_discover573_tracking

                :froggo_discover573_no_hideable
                const-string v1, "FroggoFeedDiag573"
                const-string v2, "discover hideableTokenPresent=0"
                invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                :froggo_discover573_tracking
                invoke-virtual {v0}, LX/41T;->CE5()Ljava/lang/String;
                move-result-object v1
                if-eqz v1, :froggo_discover573_no_tracking
                const-string v1, "FroggoFeedDiag573"
                const-string v2, "discover trackingPresent=1"
                invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                goto :froggo_discover573_diag_done

                :froggo_discover573_no_tracking
                const-string v1, "FroggoFeedDiag573"
                const-string v2, "discover trackingPresent=0"
                invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                :froggo_discover573_diag_done
                nop
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val hideFacebookSuggestedForYou573Patch = bytecodePatch(
    name = "Facebook 573 Feed recommendation diagnostics",
    description = "Logs structural metadata for injected Feed stories without filtering them.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        feedEdgeInsertion.method.addInstructions(
            0,
            """
                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->B6k()Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                move-result-object v0
                sget-object v1, Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;->A0E:Lcom/crossapp/graphql/facebook/enums/GraphQLFeedStoryCategory;
                if-ne v0, v1, :froggo_suggested573_diag_done

                const-string v0, "FroggoFeedDiag573"
                const-string v1, "candidate category=INJECTED_STORY"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->BO4()LX/3S1;
                move-result-object v0
                instance-of v1, v0, Lcom/facebook/graphql/model/GraphQLStory;
                if-eqz v1, :froggo_suggested573_non_story
                check-cast v0, Lcom/facebook/graphql/model/GraphQLStory;
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLStory;->A0d()LX/41R;
                move-result-object v0
                if-eqz v0, :froggo_suggested573_no_rec_ctx

                const v1, -0x1357e30f
                invoke-virtual {v0, v1}, LX/41R;->getCachedBoolean(I)Z
                move-result v0
                if-eqz v0, :froggo_suggested573_rec_ineligible
                const-string v0, "FroggoFeedDiag573"
                const-string v1, "story recommendationContext=1 eligible=1"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                goto :froggo_suggested573_check_title

                :froggo_suggested573_rec_ineligible
                const-string v0, "FroggoFeedDiag573"
                const-string v1, "story recommendationContext=1 eligible=0"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                :froggo_suggested573_check_title
                move-object/from16 v0, p2
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;->BO4()LX/3S1;
                move-result-object v0
                check-cast v0, Lcom/facebook/graphql/model/GraphQLStory;
                invoke-virtual {v0}, Lcom/facebook/graphql/model/GraphQLStory;->A0d()LX/41R;
                move-result-object v0
                const v1, 0x1254433f
                invoke-virtual {v0, v1}, LX/41R;->getCachedString(I)Ljava/lang/String;
                move-result-object v0
                if-eqz v0, :froggo_suggested573_no_title
                const-string v0, "FroggoFeedDiag573"
                const-string v1, "recommendationTitlePresent=1"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                goto :froggo_suggested573_diag_done

                :froggo_suggested573_no_title
                const-string v0, "FroggoFeedDiag573"
                const-string v1, "recommendationTitlePresent=0"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                goto :froggo_suggested573_diag_done

                :froggo_suggested573_no_rec_ctx
                const-string v0, "FroggoFeedDiag573"
                const-string v1, "story recommendationContext=0"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                goto :froggo_suggested573_diag_done

                :froggo_suggested573_non_story
                const-string v0, "FroggoFeedDiag573"
                const-string v1, "node=nonStory"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                :froggo_suggested573_diag_done
                nop
            """.trimIndent(),
        )
    }
}
