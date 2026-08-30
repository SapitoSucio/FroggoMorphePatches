package app.froggo.patches.facebook.download

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation

private val storyMoreMenu = Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/3QZ;",
        "LX/X3K;",
        "LX/BsZ;",
        "Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;",
        "LX/X3Z;",
    ),
    custom = { method, classDef ->
        classDef.type == "Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;" &&
            method.name == "A08"
    },
)

private val menuCallback = Fingerprint(
    returnType = "V",
    parameters = listOf("LX/VyQ;"),
    custom = { method, classDef ->
        classDef.type == "LX/WKI;" && method.name == "Dtf"
    },
)

private val videoSaveCallback = Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    custom = { method, classDef ->
        classDef.type == "LX/bq4;" && method.name == "onClick"
    },
)

private val reelSidebar = Fingerprint(
    returnType = "LX/3Pu;",
    parameters = listOf("LX/3QZ;"),
    custom = { method, classDef ->
        classDef.type == "LX/9vm;" && method.name == "A1K"
    },
)

private val downloadWorkerInstructions = """
    move-object/from16 v24, p0
    iget-object v24, v24, LX/WKI;->A02:Ljava/lang/Object;
    check-cast v24, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;
    iget-object v0, v24, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A09:Landroid/content/Context;
    iget-object v1, v24, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A02:Lcom/facebook/stories/model/StoryCard;
    const/4 v8, 0x0
    const/4 v16, 0x0

    :froggo_story_download_try_start
    invoke-virtual {v1}, Lcom/facebook/stories/model/StoryCard;->getMedia()LX/9Uo;
    move-result-object v2
    if-eqz v2, :froggo_story_download_fail
    invoke-virtual {v1}, Lcom/facebook/stories/model/StoryCard;->A0l()LX/8OX;
    move-result-object v3
    sget-object v5, LX/8OX;->A0D:LX/8OX;
    if-ne v3, v5, :froggo_story_download_photo
    iget-object v4, v2, LX/9Uo;->A05:Ljava/lang/String;
    const/4 v6, 0x1
    goto :froggo_story_download_type_ready

    :froggo_story_download_photo
    sget-object v5, LX/8OX;->A09:LX/8OX;
    if-ne v3, v5, :froggo_story_download_fail
    iget-object v4, v2, LX/9Uo;->A03:Ljava/lang/String;
    const/4 v6, 0x0

    :froggo_story_download_type_ready
    if-eqz v4, :froggo_story_download_fail
    invoke-virtual {v4}, Ljava/lang/String;->length()I
    move-result v5
    if-lez v5, :froggo_story_download_fail

    invoke-virtual {v1}, Lcom/facebook/stories/model/StoryCard;->A0U()LX/CPA;
    move-result-object v7
    if-eqz v7, :froggo_story_download_card_id
    invoke-interface {v7}, LX/CPA;->C2z()Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_story_download_owner_name
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v9
    if-lez v9, :froggo_story_download_owner_name
    goto :froggo_story_download_owner_ready

    :froggo_story_download_owner_name
    invoke-interface {v7}, LX/CPA;->getName()Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_story_download_card_id
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v9
    if-lez v9, :froggo_story_download_card_id
    goto :froggo_story_download_owner_ready

    :froggo_story_download_card_id
    invoke-virtual {v1}, Lcom/facebook/stories/model/StoryCard;->getId()Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_story_download_unknown_owner
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v9
    if-lez v9, :froggo_story_download_unknown_owner
    goto :froggo_story_download_owner_ready

    :froggo_story_download_unknown_owner
    const-string v5, "unknown"

    :froggo_story_download_owner_ready
    const-string v9, "@"
    invoke-virtual {v5, v9}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
    move-result v9
    if-eqz v9, :froggo_story_download_owner_no_at
    const/4 v9, 0x1
    invoke-virtual {v5, v9}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v5

    :froggo_story_download_owner_no_at
    const-string v9, "[^A-Za-z0-9._-]"
    const-string v10, "_"
    invoke-virtual {v5, v9, v10}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_story_download_unknown_owner_after_sanitize
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v9
    if-lez v9, :froggo_story_download_unknown_owner_after_sanitize
    goto :froggo_story_download_owner_sanitized

    :froggo_story_download_unknown_owner_after_sanitize
    const-string v5, "unknown"

    :froggo_story_download_owner_sanitized
    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;
    move-result-object v8
    new-instance v17, Ljava/net/URL;
    invoke-direct {v17, v4}, Ljava/net/URL;-><init>(Ljava/lang/String;)V
    invoke-virtual {v17}, Ljava/net/URL;->openConnection()Ljava/net/URLConnection;
    move-result-object v18
    check-cast v18, Ljava/net/HttpURLConnection;
    const-string v19, "User-Agent"
    const-string v20, "Mozilla/5.0"
    invoke-virtual {v18, v19, v20}, Ljava/net/HttpURLConnection;->setRequestProperty(Ljava/lang/String;Ljava/lang/String;)V
    invoke-virtual {v18}, Ljava/net/HttpURLConnection;->getResponseCode()I
    move-result v19
    const/16 v20, 0xc8
    if-lt v19, v20, :froggo_story_download_fail
    const/16 v20, 0x190
    if-ge v19, v20, :froggo_story_download_fail
    invoke-virtual {v18}, Ljava/net/HttpURLConnection;->getContentType()Ljava/lang/String;
    move-result-object v10
    if-eqz v10, :froggo_story_download_default_mime
    const-string v19, ";"
    invoke-virtual {v10, v19}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I
    move-result v20
    if-lez v20, :froggo_story_download_mime_ready
    const/4 v19, 0x0
    invoke-virtual {v10, v19, v20}, Ljava/lang/String;->substring(II)Ljava/lang/String;
    move-result-object v10

    :froggo_story_download_mime_ready
    invoke-virtual {v10}, Ljava/lang/String;->length()I
    move-result v19
    if-lez v19, :froggo_story_download_default_mime
    goto :froggo_story_download_collection

    :froggo_story_download_default_mime
    if-eqz v6, :froggo_story_download_photo_mime
    const-string v10, "video/mp4"
    goto :froggo_story_download_collection

    :froggo_story_download_photo_mime
    const-string v10, "image/jpeg"

    :froggo_story_download_collection
    if-eqz v6, :froggo_story_download_images_collection
    sget-object v9, Landroid/provider/MediaStore${'$'}Video${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    const-string v19, "Pictures/FroggoPatches/Historias/@"
    goto :froggo_story_download_path_prefix_ready

    :froggo_story_download_images_collection
    sget-object v9, Landroid/provider/MediaStore${'$'}Images${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    const-string v19, "Pictures/FroggoPatches/Historias/@"

    :froggo_story_download_path_prefix_ready
    new-instance v20, Ljava/lang/StringBuilder;
    invoke-direct {v20, v19}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v20, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v19, "/"
    invoke-virtual {v20, v19}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v20}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v11

    invoke-static {v4}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
    move-result-object v19
    invoke-virtual {v19}, Landroid/net/Uri;->getPath()Ljava/lang/String;
    move-result-object v20
    if-eqz v20, :froggo_story_download_default_extension
    const-string v19, "."
    invoke-virtual {v20, v19}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I
    move-result v21
    if-lez v21, :froggo_story_download_default_extension
    add-int/lit8 v22, v21, 0x1
    invoke-virtual {v20}, Ljava/lang/String;->length()I
    move-result v23
    if-ge v22, v23, :froggo_story_download_default_extension
    invoke-virtual {v20, v21}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v12
    sget-object v23, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-virtual {v12, v23}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;
    move-result-object v12
    goto :froggo_story_download_extension_ready

    :froggo_story_download_default_extension
    if-eqz v6, :froggo_story_download_jpg_extension
    const-string v12, ".mp4"
    goto :froggo_story_download_extension_ready

    :froggo_story_download_jpg_extension
    const-string v12, ".jpg"

    :froggo_story_download_extension_ready
    new-instance v19, Ljava/text/SimpleDateFormat;
    const-string v20, "yyyyMMdd_HHmmss"
    sget-object v21, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-direct {v19, v20, v21}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;Ljava/util/Locale;)V
    new-instance v20, Ljava/util/Date;
    invoke-direct {v20}, Ljava/util/Date;-><init>()V
    invoke-virtual {v19, v20}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;
    move-result-object v13
    const/4 v14, 0x1

    :froggo_story_download_unique_name
    const-string v19, "%s_story-%02d%s"
    const/4 v20, 0x3
    new-array v20, v20, [Ljava/lang/Object;
    const/4 v21, 0x0
    aput-object v13, v20, v21
    const/4 v21, 0x1
    invoke-static {v14}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v22
    aput-object v22, v20, v21
    const/4 v21, 0x2
    aput-object v12, v20, v21
    sget-object v21, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-static {v21, v19, v20}, Ljava/lang/String;->format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v15
    const-string v19, "_id"
    const/4 v20, 0x1
    new-array v20, v20, [Ljava/lang/String;
    const/4 v21, 0x0
    aput-object v19, v20, v21
    const-string v19, "relative_path=? AND _display_name=?"
    const/4 v21, 0x2
    new-array v21, v21, [Ljava/lang/String;
    const/4 v22, 0x0
    aput-object v11, v21, v22
    const/4 v22, 0x1
    aput-object v15, v21, v22
    const/4 v22, 0x0
    invoke-virtual {v8, v9, v20, v19, v21, v22}, Landroid/content/ContentResolver;->query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
    move-result-object v19
    if-eqz v19, :froggo_story_download_name_available
    invoke-interface {v19}, Landroid/database/Cursor;->moveToFirst()Z
    move-result v20
    invoke-interface {v19}, Landroid/database/Cursor;->close()V
    if-eqz v20, :froggo_story_download_name_available
    add-int/lit8 v14, v14, 0x1
    goto :froggo_story_download_unique_name

    :froggo_story_download_name_available
    new-instance v23, Landroid/content/ContentValues;
    invoke-direct {v23}, Landroid/content/ContentValues;-><init>()V
    const-string v19, "_display_name"
    invoke-virtual {v23, v19, v15}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v19, "mime_type"
    invoke-virtual {v23, v19, v10}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v19, "relative_path"
    invoke-virtual {v23, v19, v11}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v19, "is_pending"
    const/4 v20, 0x1
    invoke-static {v20}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v20
    invoke-virtual {v23, v19, v20}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    invoke-virtual {v8, v9, v23}, Landroid/content/ContentResolver;->insert(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;
    move-result-object v16
    if-eqz v16, :froggo_story_download_fail
    invoke-virtual {v8, v16}, Landroid/content/ContentResolver;->openOutputStream(Landroid/net/Uri;)Ljava/io/OutputStream;
    move-result-object v20
    if-eqz v20, :froggo_story_download_fail
    invoke-virtual {v18}, Ljava/net/HttpURLConnection;->getInputStream()Ljava/io/InputStream;
    move-result-object v19
    const/16 v21, 0x2000
    new-array v21, v21, [B

    :froggo_story_download_copy_loop
    invoke-virtual {v19, v21}, Ljava/io/InputStream;->read([B)I
    move-result v22
    if-lez v22, :froggo_story_download_copy_done
    const/4 v23, 0x0
    invoke-virtual {v20, v21, v23, v22}, Ljava/io/OutputStream;->write([BII)V
    goto :froggo_story_download_copy_loop

    :froggo_story_download_copy_done
    invoke-virtual {v19}, Ljava/io/InputStream;->close()V
    invoke-virtual {v20}, Ljava/io/OutputStream;->close()V
    invoke-virtual {v18}, Ljava/net/HttpURLConnection;->disconnect()V
    new-instance v23, Landroid/content/ContentValues;
    invoke-direct {v23}, Landroid/content/ContentValues;-><init>()V
    const-string v19, "is_pending"
    const/4 v20, 0x0
    invoke-static {v20}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v20
    invoke-virtual {v23, v19, v20}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    const/4 v19, 0x0
    invoke-virtual {v8, v16, v23, v19, v19}, Landroid/content/ContentResolver;->update(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
    const-string v19, "Froggo: download complete"
    const/4 v20, 0x1
    invoke-static {v0, v19, v20}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v19
    invoke-virtual {v19}, Landroid/widget/Toast;->show()V
    goto :froggo_story_download_finish

    :froggo_story_download_finish
    return-void
    .catch Ljava/lang/Throwable; {:froggo_story_download_try_start .. :froggo_story_download_finish} :froggo_story_download_catch

    :froggo_story_download_fail
    const-string v19, "FroggoPatches"
    const-string v20, "story download failed"
    invoke-static {v19, v20}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    if-eqz v16, :froggo_story_download_fail_notice
    const/4 v19, 0x0
    invoke-virtual {v8, v16, v19, v19}, Landroid/content/ContentResolver;->delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I
    const/4 v16, 0x0

    :froggo_story_download_fail_notice
    const-string v19, "Froggo: download failed"
    const/4 v20, 0x1
    invoke-static {v0, v19, v20}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v19
    invoke-virtual {v19}, Landroid/widget/Toast;->show()V
    goto :froggo_story_download_finish

    :froggo_story_download_catch
    move-exception v19
    const-string v20, "FroggoPatches"
    const-string v21, "story download exception"
    invoke-static {v20, v21, v19}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    goto :froggo_story_download_fail
""".trimIndent()

private val videoDownloadWorkerInstructions = """
    move-object/from16 v24, p0
    iget-object v24, v24, LX/bq4;->A01:LX/b1P;
    invoke-virtual {v24}, Landroid/view/View;->getContext()Landroid/content/Context;
    move-result-object v0
    const/4 v8, 0x0
    const/4 v16, 0x0
    const/4 v17, 0x0
    const/4 v18, 0x0
    const/4 v19, 0x0

    :froggo_video_download_try_start
    iget-object v1, v24, LX/a8s;->A0B:Lcom/facebook/video/engine/api/VideoPlayerParams;
    if-eqz v1, :froggo_video_download_fail
    iget-object v2, v1, Lcom/facebook/video/engine/api/VideoPlayerParams;->A0b:Lcom/facebook/video/engine/api/VideoDataSource;
    if-eqz v2, :froggo_video_download_fail
    iget-object v4, v2, Lcom/facebook/video/engine/api/VideoDataSource;->A08:Landroid/net/Uri;
    if-eqz v4, :froggo_video_download_fail
    invoke-virtual {v4}, Landroid/net/Uri;->getScheme()Ljava/lang/String;
    move-result-object v20
    if-eqz v20, :froggo_video_download_fail
    const-string v21, "http"
    invoke-virtual {v20, v21}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
    move-result v22
    if-nez v22, :froggo_video_download_http
    const-string v21, "https"
    invoke-virtual {v20, v21}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
    move-result v22
    if-nez v22, :froggo_video_download_http

    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;
    move-result-object v8
    invoke-virtual {v8, v4}, Landroid/content/ContentResolver;->getType(Landroid/net/Uri;)Ljava/lang/String;
    move-result-object v10
    invoke-virtual {v8, v4}, Landroid/content/ContentResolver;->openInputStream(Landroid/net/Uri;)Ljava/io/InputStream;
    move-result-object v18
    if-eqz v18, :froggo_video_download_fail
    goto :froggo_video_download_source_ready

    :froggo_video_download_http
    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;
    move-result-object v8
    new-instance v20, Ljava/net/URL;
    invoke-virtual {v4}, Landroid/net/Uri;->toString()Ljava/lang/String;
    move-result-object v21
    invoke-direct {v20, v21}, Ljava/net/URL;-><init>(Ljava/lang/String;)V
    invoke-virtual {v20}, Ljava/net/URL;->openConnection()Ljava/net/URLConnection;
    move-result-object v21
    check-cast v21, Ljava/net/HttpURLConnection;
    move-object/from16 v17, v21
    const-string v20, "User-Agent"
    const-string v21, "Mozilla/5.0"
    invoke-virtual {v17, v20, v21}, Ljava/net/HttpURLConnection;->setRequestProperty(Ljava/lang/String;Ljava/lang/String;)V
    invoke-virtual {v17}, Ljava/net/HttpURLConnection;->getResponseCode()I
    move-result v20
    const/16 v21, 0xc8
    if-lt v20, v21, :froggo_video_download_fail
    const/16 v21, 0x190
    if-ge v20, v21, :froggo_video_download_fail
    invoke-virtual {v17}, Ljava/net/HttpURLConnection;->getContentType()Ljava/lang/String;
    move-result-object v10
    invoke-virtual {v17}, Ljava/net/HttpURLConnection;->getInputStream()Ljava/io/InputStream;
    move-result-object v18
    if-eqz v18, :froggo_video_download_fail

    :froggo_video_download_source_ready
    if-eqz v10, :froggo_video_download_default_mime
    const-string v20, ";"
    invoke-virtual {v10, v20}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I
    move-result v21
    if-lez v21, :froggo_video_download_mime_ready
    const/4 v20, 0x0
    invoke-virtual {v10, v20, v21}, Ljava/lang/String;->substring(II)Ljava/lang/String;
    move-result-object v10

    :froggo_video_download_mime_ready
    invoke-virtual {v10}, Ljava/lang/String;->length()I
    move-result v20
    if-lez v20, :froggo_video_download_default_mime
    goto :froggo_video_download_author

    :froggo_video_download_default_mime
    const-string v10, "video/mp4"

    :froggo_video_download_author
    const-string v5, "unknown"
    iget-object v23, v24, LX/a8S;->A04:LX/4ta;
    invoke-static {v23}, LX/2lw;->A05(LX/4ta;)Lcom/facebook/graphql/model/GraphQLMedia;
    move-result-object v23
    if-eqz v23, :froggo_video_download_author_from_param
    invoke-virtual {v23}, Lcom/facebook/graphql/model/GraphQLMedia;->A0O()LX/41Q;
    move-result-object v21
    if-eqz v21, :froggo_video_download_author_media_id
    const v22, 0xf02988d6
    invoke-virtual {v21, v22}, Lcom/facebook/graphql/modelutil/BaseModelWithTree;->getCachedString(I)Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_video_download_author_name
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v20
    if-lez v20, :froggo_video_download_author_name
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_name
    const v22, 0x337a8b
    invoke-virtual {v21, v22}, Lcom/facebook/graphql/modelutil/BaseModelWithTree;->getCachedString(I)Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_video_download_author_owner_id
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v20
    if-lez v20, :froggo_video_download_author_owner_id
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_owner_id
    const v22, 0xd1b
    invoke-virtual {v21, v22}, Lcom/facebook/graphql/modelutil/BaseModelWithTree;->getCachedString(I)Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_video_download_author_media_id
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v20
    if-lez v20, :froggo_video_download_author_media_id
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_media_id
    const v22, 0xd1b
    invoke-virtual {v23, v22}, Lcom/facebook/graphql/modelutil/BaseModelWithTree;->getCachedString(I)Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_video_download_author_from_param
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v20
    if-lez v20, :froggo_video_download_author_from_param
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_from_param
    iget-object v5, v1, Lcom/facebook/video/engine/api/VideoPlayerParams;->A0v:Ljava/lang/String;
    if-eqz v5, :froggo_video_download_author_unknown
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v20
    if-lez v20, :froggo_video_download_author_unknown
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_unknown
    const-string v5, "unknown"

    :froggo_video_download_author_ready
    const-string v20, "@"
    invoke-virtual {v5, v20}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
    move-result v20
    if-eqz v20, :froggo_video_download_author_no_at
    const/4 v20, 0x1
    invoke-virtual {v5, v20}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v5

    :froggo_video_download_author_no_at
    const-string v20, "[^A-Za-z0-9._-]"
    const-string v21, "_"
    invoke-virtual {v5, v20, v21}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_video_download_author_sanitized_unknown
    invoke-virtual {v5}, Ljava/lang/String;->length()I
    move-result v20
    if-lez v20, :froggo_video_download_author_sanitized_unknown
    goto :froggo_video_download_author_sanitized

    :froggo_video_download_author_sanitized_unknown
    const-string v5, "unknown"

    :froggo_video_download_author_sanitized
    const-string v20, "Pictures/FroggoPatches/Videos/@"
    new-instance v21, Ljava/lang/StringBuilder;
    invoke-direct {v21, v20}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v21, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v20, "/"
    invoke-virtual {v21, v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v21}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v11

    invoke-virtual {v4}, Landroid/net/Uri;->getPath()Ljava/lang/String;
    move-result-object v20
    if-eqz v20, :froggo_video_download_default_extension
    const-string v21, "."
    invoke-virtual {v20, v21}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I
    move-result v22
    if-lez v22, :froggo_video_download_default_extension
    add-int/lit8 v23, v22, 0x1
    invoke-virtual {v20}, Ljava/lang/String;->length()I
    move-result v21
    if-ge v23, v21, :froggo_video_download_default_extension
    invoke-virtual {v20, v22}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v12
    sget-object v21, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-virtual {v12, v21}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;
    move-result-object v12
    goto :froggo_video_download_extension_ready

    :froggo_video_download_default_extension
    const-string v12, ".mp4"

    :froggo_video_download_extension_ready
    new-instance v20, Ljava/text/SimpleDateFormat;
    const-string v21, "yyyyMMdd_HHmmss"
    sget-object v22, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-direct {v20, v21, v22}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;Ljava/util/Locale;)V
    new-instance v21, Ljava/util/Date;
    invoke-direct {v21}, Ljava/util/Date;-><init>()V
    invoke-virtual {v20, v21}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;
    move-result-object v13
    const/4 v14, 0x1

    :froggo_video_download_unique_name
    const-string v20, "%s_video-%02d%s"
    const/4 v21, 0x3
    new-array v21, v21, [Ljava/lang/Object;
    const/4 v22, 0x0
    aput-object v13, v21, v22
    const/4 v22, 0x1
    invoke-static {v14}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v23
    aput-object v23, v21, v22
    const/4 v22, 0x2
    aput-object v12, v21, v22
    sget-object v22, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-static {v22, v20, v21}, Ljava/lang/String;->format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v15
    const-string v20, "_id"
    const/4 v21, 0x1
    new-array v21, v21, [Ljava/lang/String;
    const/4 v22, 0x0
    aput-object v20, v21, v22
    const-string v20, "relative_path=? AND _display_name=?"
    const/4 v22, 0x2
    new-array v22, v22, [Ljava/lang/String;
    const/4 v23, 0x0
    aput-object v11, v22, v23
    const/4 v23, 0x1
    aput-object v15, v22, v23
    const/4 v23, 0x0
    sget-object v9, Landroid/provider/MediaStore${'$'}Video${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    invoke-virtual {v8, v9, v21, v20, v22, v23}, Landroid/content/ContentResolver;->query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
    move-result-object v20
    if-eqz v20, :froggo_video_download_name_available
    invoke-interface {v20}, Landroid/database/Cursor;->moveToFirst()Z
    move-result v21
    invoke-interface {v20}, Landroid/database/Cursor;->close()V
    if-eqz v21, :froggo_video_download_name_available
    add-int/lit8 v14, v14, 0x1
    goto :froggo_video_download_unique_name

    :froggo_video_download_name_available
    new-instance v23, Landroid/content/ContentValues;
    invoke-direct {v23}, Landroid/content/ContentValues;-><init>()V
    const-string v20, "_display_name"
    invoke-virtual {v23, v20, v15}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v20, "mime_type"
    invoke-virtual {v23, v20, v10}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v20, "relative_path"
    invoke-virtual {v23, v20, v11}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v20, "is_pending"
    const/4 v21, 0x1
    invoke-static {v21}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v21
    invoke-virtual {v23, v20, v21}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    sget-object v20, Landroid/provider/MediaStore${'$'}Video${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    invoke-virtual {v8, v20, v23}, Landroid/content/ContentResolver;->insert(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;
    move-result-object v16
    if-eqz v16, :froggo_video_download_fail
    invoke-virtual {v8, v16}, Landroid/content/ContentResolver;->openOutputStream(Landroid/net/Uri;)Ljava/io/OutputStream;
    move-result-object v19
    if-eqz v19, :froggo_video_download_fail
    const/16 v21, 0x2000
    new-array v21, v21, [B

    :froggo_video_download_copy_loop
    invoke-virtual {v18, v21}, Ljava/io/InputStream;->read([B)I
    move-result v22
    if-lez v22, :froggo_video_download_copy_done
    const/4 v23, 0x0
    invoke-virtual {v19, v21, v23, v22}, Ljava/io/OutputStream;->write([BII)V
    goto :froggo_video_download_copy_loop

    :froggo_video_download_copy_done
    invoke-virtual {v18}, Ljava/io/InputStream;->close()V
    const/4 v18, 0x0
    invoke-virtual {v19}, Ljava/io/OutputStream;->close()V
    const/4 v19, 0x0
    if-eqz v17, :froggo_video_download_publish
    invoke-virtual {v17}, Ljava/net/HttpURLConnection;->disconnect()V
    const/4 v17, 0x0

    :froggo_video_download_publish
    new-instance v23, Landroid/content/ContentValues;
    invoke-direct {v23}, Landroid/content/ContentValues;-><init>()V
    const-string v20, "is_pending"
    const/4 v21, 0x0
    invoke-static {v21}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v21
    invoke-virtual {v23, v20, v21}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    const/4 v20, 0x0
    invoke-virtual {v8, v16, v23, v20, v20}, Landroid/content/ContentResolver;->update(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
    const-string v20, "Froggo: download complete"
    const/4 v21, 0x1
    invoke-static {v0, v20, v21}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v20
    invoke-virtual {v20}, Landroid/widget/Toast;->show()V
    goto :froggo_video_download_finish

    :froggo_video_download_finish
    return-void
    .catch Ljava/lang/Throwable; {:froggo_video_download_try_start .. :froggo_video_download_finish} :froggo_video_download_catch

    :froggo_video_download_fail
    const-string v20, "FroggoPatches"
    const-string v21, "video download failed"
    invoke-static {v20, v21}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    if-eqz v17, :froggo_video_download_delete_pending
    invoke-virtual {v17}, Ljava/net/HttpURLConnection;->disconnect()V
    const/4 v17, 0x0

    :froggo_video_download_delete_pending
    if-eqz v16, :froggo_video_download_fail_notice
    if-eqz v8, :froggo_video_download_fail_notice
    const/4 v20, 0x0
    invoke-virtual {v8, v16, v20, v20}, Landroid/content/ContentResolver;->delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I
    const/4 v16, 0x0

    :froggo_video_download_fail_notice
    const-string v20, "Froggo: download failed"
    const/4 v21, 0x1
    invoke-static {v0, v20, v21}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v20
    invoke-virtual {v20}, Landroid/widget/Toast;->show()V
    goto :froggo_video_download_finish

    :froggo_video_download_catch
    move-exception v20
    const-string v21, "FroggoPatches"
    const-string v22, "video download exception"
    invoke-static {v21, v22, v20}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    goto :froggo_video_download_fail
""".trimIndent()

private val compactStoryDownloadWorkerInstructions = """
    move-object v10, p0
    iget-object v10, v10, LX/WKI;->A02:Ljava/lang/Object;
    check-cast v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;
    iget-object v0, v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A09:Landroid/content/Context;
    iget-object v1, v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A02:Lcom/facebook/stories/model/StoryCard;
    const/4 v2, 0x0
    const/4 v3, 0x0
    const/4 v6, 0x0
    const/4 v11, 0x0
    const/4 v14, 0x0

    :froggo_story_download_try_start
    invoke-virtual {v1}, Lcom/facebook/stories/model/StoryCard;->getMedia()LX/9Uo;
    move-result-object v9
    if-eqz v9, :froggo_story_download_fail
    invoke-virtual {v1}, Lcom/facebook/stories/model/StoryCard;->A0l()LX/8OX;
    move-result-object v10
    sget-object v5, LX/8OX;->A0D:LX/8OX;
    if-ne v10, v5, :froggo_story_download_photo
    iget-object v11, v9, LX/9Uo;->A05:Ljava/lang/String;
    const/4 v8, 0x1
    goto :froggo_story_download_type_ready

    :froggo_story_download_photo
    sget-object v5, LX/8OX;->A09:LX/8OX;
    if-ne v10, v5, :froggo_story_download_fail
    iget-object v11, v9, LX/9Uo;->A03:Ljava/lang/String;
    const/4 v8, 0x0

    :froggo_story_download_type_ready
    if-eqz v11, :froggo_story_download_fail
    invoke-virtual {v11}, Ljava/lang/String;->length()I
    move-result v5
    if-lez v5, :froggo_story_download_fail

    invoke-virtual {v1}, Lcom/facebook/stories/model/StoryCard;->A0U()LX/CPA;
    move-result-object v10
    if-eqz v10, :froggo_story_download_card_id
    invoke-interface {v10}, LX/CPA;->C2z()Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_story_download_owner_name
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v5
    if-lez v5, :froggo_story_download_owner_name
    goto :froggo_story_download_owner_ready

    :froggo_story_download_owner_name
    invoke-interface {v10}, LX/CPA;->getName()Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_story_download_card_id
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v5
    if-lez v5, :froggo_story_download_card_id
    goto :froggo_story_download_owner_ready

    :froggo_story_download_card_id
    invoke-virtual {v1}, Lcom/facebook/stories/model/StoryCard;->getId()Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_story_download_unknown_owner
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v5
    if-lez v5, :froggo_story_download_unknown_owner
    goto :froggo_story_download_owner_ready

    :froggo_story_download_unknown_owner
    const-string v12, "unknown"

    :froggo_story_download_owner_ready
    const-string v10, "@"
    invoke-virtual {v12, v10}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
    move-result v5
    if-eqz v5, :froggo_story_download_owner_no_at
    const/4 v10, 0x1
    invoke-virtual {v12, v10}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v12

    :froggo_story_download_owner_no_at
    const-string v10, "[^A-Za-z0-9._-]"
    const-string v5, "_"
    invoke-virtual {v12, v10, v5}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_story_download_unknown_owner_after_sanitize
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v5
    if-lez v5, :froggo_story_download_unknown_owner_after_sanitize
    goto :froggo_story_download_owner_sanitized

    :froggo_story_download_unknown_owner_after_sanitize
    const-string v12, "unknown"

    :froggo_story_download_owner_sanitized
    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;
    move-result-object v2
    new-instance v10, Ljava/net/URL;
    invoke-direct {v10, v11}, Ljava/net/URL;-><init>(Ljava/lang/String;)V
    invoke-virtual {v10}, Ljava/net/URL;->openConnection()Ljava/net/URLConnection;
    move-result-object v14
    check-cast v14, Ljava/net/HttpURLConnection;
    const-string v5, "User-Agent"
    const-string v6, "Mozilla/5.0"
    invoke-virtual {v14, v5, v6}, Ljava/net/HttpURLConnection;->setRequestProperty(Ljava/lang/String;Ljava/lang/String;)V
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->getResponseCode()I
    move-result v5
    const/16 v6, 0xc8
    if-lt v5, v6, :froggo_story_download_fail
    const/16 v6, 0x190
    if-ge v5, v6, :froggo_story_download_fail
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->getContentType()Ljava/lang/String;
    move-result-object v4
    if-eqz v4, :froggo_story_download_default_mime
    const-string v10, ";"
    invoke-virtual {v4, v10}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I
    move-result v5
    if-lez v5, :froggo_story_download_mime_ready
    const/4 v10, 0x0
    invoke-virtual {v4, v10, v5}, Ljava/lang/String;->substring(II)Ljava/lang/String;
    move-result-object v4

    :froggo_story_download_mime_ready
    invoke-virtual {v4}, Ljava/lang/String;->length()I
    move-result v5
    if-lez v5, :froggo_story_download_default_mime
    goto :froggo_story_download_collection

    :froggo_story_download_default_mime
    if-eqz v8, :froggo_story_download_photo_mime
    const-string v4, "video/mp4"
    goto :froggo_story_download_collection

    :froggo_story_download_photo_mime
    const-string v4, "image/jpeg"

    :froggo_story_download_collection
    if-eqz v8, :froggo_story_download_images_collection
    sget-object v9, Landroid/provider/MediaStore${'$'}Video${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    const-string v5, "Pictures/FroggoPatches/Historias/@"
    goto :froggo_story_download_path_prefix_ready

    :froggo_story_download_images_collection
    sget-object v9, Landroid/provider/MediaStore${'$'}Images${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    const-string v5, "Pictures/FroggoPatches/Historias/@"

    :froggo_story_download_path_prefix_ready
    new-instance v10, Ljava/lang/StringBuilder;
    invoke-direct {v10, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v10, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v5, "/"
    invoke-virtual {v10, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v7
    move-object v1, v7

    invoke-static {v11}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
    move-result-object v10
    invoke-virtual {v10}, Landroid/net/Uri;->getPath()Ljava/lang/String;
    move-result-object v11
    if-eqz v11, :froggo_story_download_default_extension
    const-string v5, "."
    invoke-virtual {v11, v5}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I
    move-result v10
    if-lez v10, :froggo_story_download_default_extension
    add-int/lit8 v6, v10, 0x1
    invoke-virtual {v11}, Ljava/lang/String;->length()I
    move-result v5
    if-ge v6, v5, :froggo_story_download_default_extension
    invoke-virtual {v11, v10}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v13
    sget-object v5, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-virtual {v13, v5}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;
    move-result-object v13
    goto :froggo_story_download_extension_ready

    :froggo_story_download_default_extension
    if-eqz v8, :froggo_story_download_jpg_extension
    const-string v13, ".mp4"
    goto :froggo_story_download_extension_ready

    :froggo_story_download_jpg_extension
    const-string v13, ".jpg"

    :froggo_story_download_extension_ready
    move-object p0, v13
    new-instance v10, Ljava/text/SimpleDateFormat;
    const-string v11, "yyyyMMdd_HHmmss"
    sget-object v5, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-direct {v10, v11, v5}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;Ljava/util/Locale;)V
    new-instance v11, Ljava/util/Date;
    invoke-direct {v11}, Ljava/util/Date;-><init>()V
    invoke-virtual {v10, v11}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;
    move-result-object v12
    const/4 v8, 0x1

    :froggo_story_download_unique_name
    const-string v10, "%s_story-%02d%s"
    const/4 v11, 0x3
    new-array v11, v11, [Ljava/lang/Object;
    const/4 v5, 0x0
    aput-object v12, v11, v5
    const/4 v5, 0x1
    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v13
    aput-object v13, v11, v5
    const/4 v5, 0x2
    aput-object p0, v11, v5
    sget-object v13, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-static {v13, v10, v11}, Ljava/lang/String;->format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v13

    const-string v10, "_id"
    const/4 v11, 0x1
    new-array v11, v11, [Ljava/lang/String;
    const/4 v5, 0x0
    aput-object v10, v11, v5
    const-string v10, "relative_path=? AND _display_name=?"
    const/4 v6, 0x2
    new-array v6, v6, [Ljava/lang/String;
    const/4 v5, 0x0
    aput-object v1, v6, v5
    const/4 v5, 0x1
    aput-object v13, v6, v5
    move-object v3, v9
    move-object v9, v4
    move-object v4, v11
    move-object v5, v10
    move-object v6, v6
    const/4 v7, 0x0
    invoke-virtual/range {v2 .. v7}, Landroid/content/ContentResolver;->query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
    move-result-object v10
    const/4 v11, 0x0
    if-eqz v10, :froggo_story_download_name_available
    invoke-interface {v10}, Landroid/database/Cursor;->moveToFirst()Z
    move-result v6
    invoke-interface {v10}, Landroid/database/Cursor;->close()V
    if-eqz v6, :froggo_story_download_name_available
    add-int/lit8 v8, v8, 0x1
    goto :froggo_story_download_unique_name

    :froggo_story_download_name_available
    new-instance v11, Landroid/content/ContentValues;
    invoke-direct {v11}, Landroid/content/ContentValues;-><init>()V
    const-string v10, "_display_name"
    invoke-virtual {v11, v10, v13}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v10, "mime_type"
    invoke-virtual {v11, v10, v9}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v10, "relative_path"
    invoke-virtual {v11, v10, v1}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v10, "is_pending"
    const/4 v5, 0x1
    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v5
    invoke-virtual {v11, v10, v5}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    invoke-virtual {v2, v3, v11}, Landroid/content/ContentResolver;->insert(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;
    move-result-object v11
    if-eqz v11, :froggo_story_download_fail
    invoke-virtual {v2, v11}, Landroid/content/ContentResolver;->openOutputStream(Landroid/net/Uri;)Ljava/io/OutputStream;
    move-result-object v5
    if-eqz v5, :froggo_story_download_fail
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->getInputStream()Ljava/io/InputStream;
    move-result-object v6
    if-eqz v6, :froggo_story_download_fail
    const/16 v7, 0x2000
    new-array v7, v7, [B

    :froggo_story_download_copy_loop
    invoke-virtual {v6, v7}, Ljava/io/InputStream;->read([B)I
    move-result v10
    if-lez v10, :froggo_story_download_copy_done
    const/4 v4, 0x0
    invoke-virtual {v5, v7, v4, v10}, Ljava/io/OutputStream;->write([BII)V
    goto :froggo_story_download_copy_loop

    :froggo_story_download_copy_done
    invoke-virtual {v6}, Ljava/io/InputStream;->close()V
    invoke-virtual {v5}, Ljava/io/OutputStream;->close()V
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->disconnect()V
    new-instance v10, Landroid/content/ContentValues;
    invoke-direct {v10}, Landroid/content/ContentValues;-><init>()V
    const-string v4, "is_pending"
    const/4 v5, 0x0
    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v5
    invoke-virtual {v10, v4, v5}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    const/4 v4, 0x0
    invoke-virtual {v2, v11, v10, v4, v4}, Landroid/content/ContentResolver;->update(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
    goto :froggo_story_download_finish

    :froggo_story_download_finish
    return-void
    .catch Ljava/lang/Throwable; {:froggo_story_download_try_start .. :froggo_story_download_finish} :froggo_story_download_catch

    :froggo_story_download_fail
    const-string v10, "FroggoPatches"
    const-string v5, "story download failed"
    invoke-static {v10, v5}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    if-eqz v14, :froggo_story_download_pending
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->disconnect()V
    const/4 v14, 0x0

    :froggo_story_download_pending
    if-eqz v11, :froggo_story_download_fail_notice
    const/4 v4, 0x0
    invoke-virtual {v2, v11, v4, v4}, Landroid/content/ContentResolver;->delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I
    const/4 v11, 0x0

    :froggo_story_download_fail_notice
    goto :froggo_story_download_finish

    :froggo_story_download_catch
    move-exception v5
    const-string v10, "FroggoPatches"
    const-string v4, "story download exception"
    invoke-static {v10, v4, v5}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    goto :froggo_story_download_fail
""".trimIndent()

private val compactVideoDownloadWorkerInstructions = """
    move-object v1, p0
    iget-object v1, v1, LX/bq4;->A01:LX/b1P;
    invoke-virtual {v1}, Landroid/view/View;->getContext()Landroid/content/Context;
    move-result-object v0
    const-string v10, "FroggoPatches"
    const-string v11, "worker-start"
    invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    const/4 v5, 0x0
    const/4 v6, 0x0
    const/4 v9, 0x0
    const/4 v14, 0x0

    :froggo_video_download_try_start
    iget-object v2, v1, LX/a8s;->A0B:Lcom/facebook/video/engine/api/VideoPlayerParams;
    if-eqz v2, :froggo_video_download_fail
    iget-object v3, v2, Lcom/facebook/video/engine/api/VideoPlayerParams;->A0b:Lcom/facebook/video/engine/api/VideoDataSource;
    if-eqz v3, :froggo_video_download_fail
    iget-object v4, v3, Lcom/facebook/video/engine/api/VideoDataSource;->A08:Landroid/net/Uri;
    if-eqz v4, :froggo_video_download_fail
    const-string v10, "FroggoPatches"
    const-string v11, "uri-ready"
    invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;
    move-result-object v5
    invoke-virtual {v4}, Landroid/net/Uri;->getScheme()Ljava/lang/String;
    move-result-object v10
    if-eqz v10, :froggo_video_download_fail
    const-string v11, "http"
    invoke-virtual {v10, v11}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
    move-result v12
    if-nez v12, :froggo_video_download_http
    const-string v11, "https"
    invoke-virtual {v10, v11}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
    move-result v12
    if-nez v12, :froggo_video_download_http

    invoke-virtual {v5, v4}, Landroid/content/ContentResolver;->getType(Landroid/net/Uri;)Ljava/lang/String;
    move-result-object v11
    invoke-virtual {v5, v4}, Landroid/content/ContentResolver;->openInputStream(Landroid/net/Uri;)Ljava/io/InputStream;
    move-result-object v7
    if-eqz v7, :froggo_video_download_fail
    goto :froggo_video_download_source_ready

    :froggo_video_download_http
    const-string v10, "FroggoPatches"
    const-string v11, "before-url"
    invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    new-instance v10, Ljava/net/URL;
    invoke-virtual {v4}, Landroid/net/Uri;->toString()Ljava/lang/String;
    move-result-object v11
    invoke-direct {v10, v11}, Ljava/net/URL;-><init>(Ljava/lang/String;)V
    invoke-virtual {v10}, Ljava/net/URL;->openConnection()Ljava/net/URLConnection;
    move-result-object v11
    check-cast v11, Ljava/net/HttpURLConnection;
    move-object v14, v11
    const-string v10, "User-Agent"
    const-string v11, "Mozilla/5.0"
    invoke-virtual {v14, v10, v11}, Ljava/net/HttpURLConnection;->setRequestProperty(Ljava/lang/String;Ljava/lang/String;)V
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->getResponseCode()I
    move-result v10
    const/16 v11, 0xc8
    if-lt v10, v11, :froggo_video_download_fail
    const/16 v11, 0x190
    if-ge v10, v11, :froggo_video_download_fail
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->getContentType()Ljava/lang/String;
    move-result-object v11
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->getInputStream()Ljava/io/InputStream;
    move-result-object v7
    if-eqz v7, :froggo_video_download_fail
    const-string v10, "FroggoPatches"
    const-string v11, "input-ready"
    invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    :froggo_video_download_source_ready
    if-eqz v11, :froggo_video_download_default_mime
    const-string v10, ";"
    invoke-virtual {v11, v10}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I
    move-result v12
    if-lez v12, :froggo_video_download_mime_ready
    const/4 v10, 0x0
    invoke-virtual {v11, v10, v12}, Ljava/lang/String;->substring(II)Ljava/lang/String;
    move-result-object v11

    :froggo_video_download_mime_ready
    invoke-virtual {v11}, Ljava/lang/String;->length()I
    move-result v12
    if-lez v12, :froggo_video_download_default_mime
    goto :froggo_video_download_author

    :froggo_video_download_default_mime
    const-string v11, "video/mp4"

    :froggo_video_download_author
    const-string v10, "FroggoPatches"
    const-string v12, "before-author"
    invoke-static {v10, v12}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    move-object v3, v11
    const-string v12, "unknown"
    iget-object v10, v1, LX/a8S;->A04:LX/4ta;
    invoke-static {v10}, LX/2lw;->A05(LX/4ta;)Lcom/facebook/graphql/model/GraphQLMedia;
    move-result-object v10
    if-eqz v10, :froggo_video_download_author_from_param
    invoke-virtual {v10}, Lcom/facebook/graphql/model/GraphQLMedia;->A0O()LX/41Q;
    move-result-object v11
    if-eqz v11, :froggo_video_download_author_media_id
    const v13, 0xf02988d6
    invoke-virtual {v11, v13}, Lcom/facebook/graphql/modelutil/BaseModelWithTree;->getCachedString(I)Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_video_download_author_name
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v13
    if-lez v13, :froggo_video_download_author_name
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_name
    const v13, 0x337a8b
    invoke-virtual {v11, v13}, Lcom/facebook/graphql/modelutil/BaseModelWithTree;->getCachedString(I)Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_video_download_author_owner_id
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v13
    if-lez v13, :froggo_video_download_author_owner_id
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_owner_id
    const v13, 0xd1b
    invoke-virtual {v11, v13}, Lcom/facebook/graphql/modelutil/BaseModelWithTree;->getCachedString(I)Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_video_download_author_media_id
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v13
    if-lez v13, :froggo_video_download_author_media_id
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_media_id
    const v13, 0xd1b
    invoke-virtual {v10, v13}, Lcom/facebook/graphql/modelutil/BaseModelWithTree;->getCachedString(I)Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_video_download_author_from_param
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v13
    if-lez v13, :froggo_video_download_author_from_param
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_from_param
    iget-object v12, v2, Lcom/facebook/video/engine/api/VideoPlayerParams;->A0v:Ljava/lang/String;
    if-eqz v12, :froggo_video_download_author_unknown
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v13
    if-lez v13, :froggo_video_download_author_unknown
    goto :froggo_video_download_author_ready

    :froggo_video_download_author_unknown
    const-string v12, "unknown"

    :froggo_video_download_author_ready
    const-string v10, "@"
    invoke-virtual {v12, v10}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
    move-result v13
    if-eqz v13, :froggo_video_download_author_no_at
    const/4 v10, 0x1
    invoke-virtual {v12, v10}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v12

    :froggo_video_download_author_no_at
    const-string v10, "[^A-Za-z0-9._-]"
    const-string v11, "_"
    invoke-virtual {v12, v10, v11}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v12
    if-eqz v12, :froggo_video_download_author_sanitized_unknown
    invoke-virtual {v12}, Ljava/lang/String;->length()I
    move-result v13
    if-lez v13, :froggo_video_download_author_sanitized_unknown
    goto :froggo_video_download_author_sanitized

    :froggo_video_download_author_sanitized_unknown
    const-string v12, "unknown"

    :froggo_video_download_author_sanitized
    const-string v10, "FroggoPatches"
    const-string v11, "before-mediastore"
    invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    const-string v10, "Pictures/FroggoPatches/Videos/@"
    new-instance v11, Ljava/lang/StringBuilder;
    invoke-direct {v11, v10}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v10, "/"
    invoke-virtual {v11, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v11}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v13
    move-object p0, v13

    invoke-virtual {v4}, Landroid/net/Uri;->getPath()Ljava/lang/String;
    move-result-object v10
    if-eqz v10, :froggo_video_download_default_extension
    const-string v11, "."
    invoke-virtual {v10, v11}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I
    move-result v13
    if-lez v13, :froggo_video_download_default_extension
    add-int/lit8 v12, v13, 0x1
    invoke-virtual {v10}, Ljava/lang/String;->length()I
    move-result v11
    if-ge v12, v11, :froggo_video_download_default_extension
    invoke-virtual {v10, v13}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v2
    sget-object v11, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-virtual {v2, v11}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;
    move-result-object v2
    goto :froggo_video_download_extension_ready

    :froggo_video_download_default_extension
    const-string v2, ".mp4"

    :froggo_video_download_extension_ready
    new-instance v10, Ljava/text/SimpleDateFormat;
    const-string v11, "yyyyMMdd_HHmmss"
    sget-object v13, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-direct {v10, v11, v13}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;Ljava/util/Locale;)V
    new-instance v11, Ljava/util/Date;
    invoke-direct {v11}, Ljava/util/Date;-><init>()V
    invoke-virtual {v10, v11}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;
    move-result-object v1
    const/4 v12, 0x1
    move-object v4, v7

    :froggo_video_download_unique_name
    const-string v10, "%s_video-%02d%s"
    const/4 v11, 0x3
    new-array v11, v11, [Ljava/lang/Object;
    const/4 v8, 0x0
    aput-object v1, v11, v8
    const/4 v8, 0x1
    invoke-static {v12}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v13
    aput-object v13, v11, v8
    const/4 v8, 0x2
    aput-object v2, v11, v8
    sget-object v13, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-static {v13, v10, v11}, Ljava/lang/String;->format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v13

    sget-object v10, Landroid/provider/MediaStore${'$'}Video${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    move-object v6, v10
    const-string v10, "_id"
    const/4 v11, 0x1
    new-array v11, v11, [Ljava/lang/String;
    const/4 v8, 0x0
    aput-object v10, v11, v8
    const-string v10, "relative_path=? AND _display_name=?"
    const/4 v9, 0x2
    new-array v9, v9, [Ljava/lang/String;
    const/4 v8, 0x0
    aput-object p0, v9, v8
    const/4 v8, 0x1
    aput-object v13, v9, v8
    move-object v7, v11
    move-object v8, v10
    const/4 v10, 0x0
    const/4 v11, 0x0
    invoke-virtual/range {v5 .. v10}, Landroid/content/ContentResolver;->query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
    move-result-object v10
    if-eqz v10, :froggo_video_download_name_available
    invoke-interface {v10}, Landroid/database/Cursor;->moveToFirst()Z
    move-result v9
    invoke-interface {v10}, Landroid/database/Cursor;->close()V
    if-eqz v9, :froggo_video_download_name_available
    add-int/lit8 v12, v12, 0x1
    goto :froggo_video_download_unique_name

    :froggo_video_download_name_available
    new-instance v7, Landroid/content/ContentValues;
    invoke-direct {v7}, Landroid/content/ContentValues;-><init>()V
    const-string v10, "_display_name"
    invoke-virtual {v7, v10, v13}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v10, "mime_type"
    invoke-virtual {v7, v10, v3}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v10, "relative_path"
    invoke-virtual {v7, v10, p0}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v10, "is_pending"
    const/4 v8, 0x1
    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v8
    invoke-virtual {v7, v10, v8}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    invoke-virtual {v5, v6, v7}, Landroid/content/ContentResolver;->insert(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;
    move-result-object v11
    if-eqz v11, :froggo_video_download_fail
    const-string v10, "FroggoPatches"
    const-string v12, "inserted"
    invoke-static {v10, v12}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    invoke-virtual {v5, v11}, Landroid/content/ContentResolver;->openOutputStream(Landroid/net/Uri;)Ljava/io/OutputStream;
    move-result-object v8
    if-eqz v8, :froggo_video_download_fail
    const/16 v10, 0x2000
    new-array v10, v10, [B

    :froggo_video_download_copy_loop
    invoke-virtual {v4, v10}, Ljava/io/InputStream;->read([B)I
    move-result v7
    if-lez v7, :froggo_video_download_copy_done
    const/4 v1, 0x0
    invoke-virtual {v8, v10, v1, v7}, Ljava/io/OutputStream;->write([BII)V
    goto :froggo_video_download_copy_loop

    :froggo_video_download_copy_done
    invoke-virtual {v4}, Ljava/io/InputStream;->close()V
    invoke-virtual {v8}, Ljava/io/OutputStream;->close()V
    if-eqz v14, :froggo_video_download_publish
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->disconnect()V
    const/4 v14, 0x0

    :froggo_video_download_publish
    new-instance v7, Landroid/content/ContentValues;
    invoke-direct {v7}, Landroid/content/ContentValues;-><init>()V
    const-string v10, "is_pending"
    const/4 v8, 0x0
    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v8
    invoke-virtual {v7, v10, v8}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    const/4 v1, 0x0
    invoke-virtual {v5, v11, v7, v1, v1}, Landroid/content/ContentResolver;->update(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
    const-string v10, "FroggoPatches"
    const-string v11, "complete"
    invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    goto :froggo_video_download_finish

    :froggo_video_download_finish
    return-void
    .catch Ljava/lang/Throwable; {:froggo_video_download_try_start .. :froggo_video_download_finish} :froggo_video_download_catch

    :froggo_video_download_fail
    const-string v1, "FroggoPatches"
    const-string v2, "video download failed"
    invoke-static {v1, v2}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    if-eqz v14, :froggo_video_download_delete_pending
    invoke-virtual {v14}, Ljava/net/HttpURLConnection;->disconnect()V
    const/4 v14, 0x0

    :froggo_video_download_delete_pending
    if-eqz v11, :froggo_video_download_fail_notice
    const/4 v1, 0x0
    invoke-virtual {v5, v11, v1, v1}, Landroid/content/ContentResolver;->delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I
    const/4 v11, 0x0

    :froggo_video_download_fail_notice
    goto :froggo_video_download_finish

    :froggo_video_download_catch
    move-exception v1
    const-string v2, "FroggoPatches"
    const-string v3, "video download exception"
    invoke-static {v2, v3, v1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    goto :froggo_video_download_fail
""".trimIndent()

private val compactReelDownloadWorkerInstructions = run {
    val initialVideoBlock = """
        move-object v1, p0
        iget-object v1, v1, LX/bq4;->A01:LX/b1P;
        invoke-virtual {v1}, Landroid/view/View;->getContext()Landroid/content/Context;
        move-result-object v0
    """.trimIndent()
    val initialReelBlock = """
        move-object v1, p0
        iget-object v1, v1, LX/WKI;->A00:Ljava/lang/Object;
        check-cast v1, LX/3QZ;
        iget-object v0, v1, LX/3QZ;->A0C:Landroid/content/Context;
        move-object v1, p0
        iget-object v1, v1, LX/WKI;->A01:Ljava/lang/Object;
        check-cast v1, LX/4ta;
    """.trimIndent()
    val transformed = compactVideoDownloadWorkerInstructions
        .replace(initialVideoBlock, initialReelBlock)
        .replace(
            "iget-object v2, v1, LX/a8s;->A0B:Lcom/facebook/video/engine/api/VideoPlayerParams;",
            "iget-object v2, v1, LX/4ta;->A03:Lcom/facebook/video/engine/api/VideoPlayerParams;",
        )
    val prefix = transformed.substringBefore("const-string v12, \"unknown\"")
    val suffix = transformed.substringAfterLast(":froggo_video_download_author_ready")
    val authorBlock = """
        const-string v12, "unknown"
        move-object v10, p0
        iget-object v10, v10, LX/WKI;->A02:Ljava/lang/Object;
        if-eqz v10, :froggo_video_download_author_unknown
        check-cast v10, LX/BsO;
        invoke-static {v10}, LX/6IH;->A00(LX/BsO;)Lcom/facebook/graphql/model/GraphQLStory;
        move-result-object v12
        if-eqz v12, :froggo_video_download_author_fallback
        invoke-virtual {v12}, Lcom/facebook/graphql/model/GraphQLStory;->A0k()Lcom/google/common/collect/ImmutableList;
        move-result-object v11
        if-eqz v11, :froggo_video_download_author_fallback
        invoke-virtual {v11}, Lcom/google/common/collect/ImmutableList;->isEmpty()Z
        move-result v13
        if-nez v13, :froggo_video_download_author_fallback
        const/4 v13, 0x0
        invoke-virtual {v11, v13}, Lcom/google/common/collect/ImmutableList;->get(I)Ljava/lang/Object;
        move-result-object v10
        check-cast v10, Lcom/facebook/graphql/modelutil/BaseModelWithTree;
        invoke-static {v10}, LX/19i;->A0y(Lcom/facebook/graphql/modelutil/BaseModelWithTree;)Ljava/lang/String;
        move-result-object v12
        if-eqz v12, :froggo_video_download_author_fallback
        invoke-virtual {v12}, Ljava/lang/String;->length()I
        move-result v13
        if-lez v13, :froggo_video_download_author_fallback
        goto :froggo_video_download_author_ready

        :froggo_video_download_author_fallback
        move-object v10, p0
        iget-object v10, v10, LX/WKI;->A02:Ljava/lang/Object;
        check-cast v10, LX/a7W;
        invoke-static {v10}, LX/19i;->A17(LX/a7W;)Ljava/lang/String;
        move-result-object v12
        if-eqz v12, :froggo_video_download_author_unknown
        invoke-virtual {v12}, Ljava/lang/String;->length()I
        move-result v13
        if-lez v13, :froggo_video_download_author_ready

        :froggo_video_download_author_unknown
        const-string v12, "unknown"

        :froggo_video_download_author_ready
    """.trimIndent()
    (prefix + authorBlock + suffix)
        .replace("froggo_video_download_", "froggo_reel_download_")
        .replace("Pictures/FroggoPatches/Videos/@", "Pictures/FroggoPatches/Reels/@")
        .replace("\"%s_video-%02d%s\"", "\"%s_reel-%02d%s\"")
}

@Suppress("unused")
val downloadFacebookMedia573Patch = bytecodePatch(
    name = "Download Facebook Media (573)",
    description = "Adds direct downloads for the visible Story, Reel, and video media through MediaStore.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        menuCallback.method.addInstructions(
            0,
            """
                iget v0, p0, LX/WKI;->${'$'}t:I
                const/16 v1, 0x7f
                if-ne v0, v1, :froggo_story_download_stock_callback
                new-instance v0, Ljava/lang/Thread;
                invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                return-void
                :froggo_story_download_stock_callback
            """.trimIndent(),
        )

        val callbackClass = menuCallback.classDef
        require(callbackClass.interfaces.add("Ljava/lang/Runnable;")) {
            "WKI already implements the Story download worker interface"
        }
        require(callbackClass.interfaces.add("Lkotlin/jvm/functions/Function1;")) {
            "WKI already implements the Reels download callback interface"
        }
        val workerMethod = ImmutableMethod(
            callbackClass.type,
            "run",
            emptyList(),
            "V",
            AccessFlags.PUBLIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget v0, p0, LX/WKI;->${'$'}t:I
                    const/16 v1, 0x7f
                    if-eq v0, v1, :froggo_story_download_worker
                    $compactReelDownloadWorkerInstructions
                    :froggo_story_download_worker
                    $compactStoryDownloadWorkerInstructions
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(workerMethod)

        val callbackInvokeMethod = ImmutableMethod(
            callbackClass.type,
            "invoke",
            listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)),
            "Ljava/lang/Object;",
            AccessFlags.PUBLIC.value,
            null,
            null,
            MutableMethodImplementation(5),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget v0, p0, LX/WKI;->${'$'}t:I
                    const/16 v1, 0x81
                    if-ne v0, v1, :froggo_reel_download_invoke_noop
                    const-string v1, "FroggoPatches"
                    const-string v2, "invoke-start"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    :froggo_reel_download_invoke_noop
                    sget-object v0, LX/0FI;->A00:LX/0FI;
                    return-object v0
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(callbackInvokeMethod)

        val videoCallbackClass = videoSaveCallback.classDef
        require(videoCallbackClass.interfaces.add("Ljava/lang/Runnable;")) {
            "Video save callback already implements the Froggo download worker interface"
        }
        val videoWorkerMethod = ImmutableMethod(
            videoCallbackClass.type,
            "run",
            emptyList(),
            "V",
            AccessFlags.PUBLIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(0, compactVideoDownloadWorkerInstructions)
        }
        videoCallbackClass.methods.add(videoWorkerMethod)
        videoSaveCallback.method.addInstructions(
            0,
            """
                new-instance v0, Ljava/lang/Thread;
                invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                return-void
            """.trimIndent(),
        )

        val reelDownloadHelper = ImmutableMethod(
            reelSidebar.classDef.type,
            "froggoCreateReelDownloadAction",
            listOf(
                ImmutableMethodParameter("Lcom/facebook/auth/usersession/FbUserSession;", null, null),
                ImmutableMethodParameter("LX/3QZ;", null, null),
                ImmutableMethodParameter("LX/4ta;", null, null),
                ImmutableMethodParameter("LX/BsO;", null, null),
            ),
            "LX/3Pu;",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(32),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    new-instance v0, LX/WKI;
                    const/16 v1, 0x80
                    move-object/from16 v2, p1
                    move-object/from16 v3, p2
                    move-object/from16 v4, p3
                    invoke-direct {v0, v1, v2, v3, v4}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V

                    new-instance v1, LX/WKI;
                    const/16 v2, 0x81
                    move-object/from16 v3, p1
                    move-object/from16 v4, p2
                    move-object/from16 v5, p3
                    invoke-direct {v1, v2, v3, v4, v5}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V

                    new-instance v2, LX/2vk;
                    const-string v3, "Download"
                    move-object v4, v0
                    invoke-direct {v2, v3, v4}, LX/2vk;-><init>(Ljava/lang/String;Lkotlin/jvm/functions/Function1;)V

                    new-instance v3, LX/2QZ;
                    const-string v4, "Download"
                    move-object v5, v1
                    invoke-direct {v3, v4, v5}, LX/2QZ;-><init>(Ljava/lang/String;Lkotlin/jvm/functions/Function1;)V

                    new-instance v4, LX/2QZ;
                    const-string v5, "Download"
                    move-object v6, v1
                    invoke-direct {v4, v5, v6}, LX/2QZ;-><init>(Ljava/lang/String;Lkotlin/jvm/functions/Function1;)V

                    new-instance v5, LX/9yX;
                    sget-object v6, LX/1Vq;->A80:LX/1Vq;
                    invoke-direct {v5, v6}, LX/9yX;-><init>(LX/1Vq;)V

                    move-object/from16 v6, p0
                    sget-object v7, LX/1c6;->A02:LX/1c6;
                    move-object v8, v2
                    move-object v9, v3
                    move-object v10, v4
                    move-object v11, v5
                    sget-object v12, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    sget-object v13, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                    const-string v14, "download_button"
                    const/4 v15, 0x0
                    const-string v16, "Download"
                    move-object/from16 v17, v0
                    const/16 v18, 0x0
                    const/16 v19, 0x0
                    const/16 v20, 0x0
                    const/16 v21, 0x11
                    const/16 v22, 0x1
                    const/16 v23, 0x0
                    const/16 v24, 0x0
                    invoke-static/range {v6 .. v24}, LX/2iZ;->A00(Lcom/facebook/auth/usersession/FbUserSession;LX/1c6;LX/2vk;LX/2QZ;LX/2QZ;LX/C8v;Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;IZZZ)LX/9yY;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
        }
        reelSidebar.classDef.methods.add(reelDownloadHelper)

        val reelSidebarBuildCalls = reelSidebar.method.implementation!!.instructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                reference?.definingClass == "LX/9yh;" &&
                    reference.name == "A01" &&
                    reference.parameterTypes.size == 17
            ) {
                index
            } else {
                null
            }
        }
        require(reelSidebarBuildCalls.size == 1) {
            "Expected one UDD sidebar builder finalization in A1K"
        }
        reelSidebar.method.addInstructions(
            reelSidebarBuildCalls.single(),
            """
                move-object/from16 v0, p1
                move-object/from16 v1, v14
                move-object/from16 v2, v37
                move-object/from16 v3, v94
                invoke-static {v0, v1, v2, v3}, LX/9vm;->froggoCreateReelDownloadAction(Lcom/facebook/auth/usersession/FbUserSession;LX/3QZ;LX/4ta;LX/BsO;)LX/3Pu;
                move-result-object v0
                move-object/from16 v1, v33
                invoke-virtual {v1, v0}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z
                move-object/from16 v1, v31
                sget-object v2, LX/1Vq;->A80:LX/1Vq;
                invoke-static {v2}, LX/9yV;->A00(LX/1Vq;)LX/7w5;
                move-result-object v2
                invoke-virtual {v1, v2}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z
                move-object/from16 v1, v32
                const-string v2, "DOWNLOAD"
                invoke-virtual {v1, v2}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z
            """.trimIndent(),
        )

        val menuDownloadHelper = ImmutableMethod(
            storyMoreMenu.classDef.type,
            "froggoCreateStoryDownload",
            listOf(
                ImmutableMethodParameter(storyMoreMenu.classDef.type, null, null),
            ),
            "LX/VyQ;",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    new-instance v0, LX/WKI;
                    const/16 v1, 0x7f
                    move-object v2, p0
                    move-object v3, p0
                    move-object v4, p0
                    invoke-direct {v0, v1, v2, v3, v4}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    move-object v5, v0
                    new-instance v0, LX/VyQ;
                    move-object v1, v5
                    sget-object v2, LX/1Vq;->A80:LX/1Vq;
                    const/4 v3, 0x0
                    const/4 v4, 0x0
                    const/4 v5, 0x0
                    const/4 v6, 0x0
                    const-string v7, "Download"
                    const/4 v8, 0x0
                    const/4 v9, 0x0
                    const/4 v10, 0x0
                    invoke-direct/range {v0 .. v10}, LX/VyQ;-><init>(LX/Wzq;LX/1Vq;Ljava/lang/CharSequence;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/String;Ljava/lang/String;IZ)V
                    return-object v0
                """.trimIndent(),
            )
        }
        storyMoreMenu.classDef.methods.add(menuDownloadHelper)

        val menuInstructions = storyMoreMenu.method.implementation!!.instructions
        val buildCalls = menuInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                reference?.definingClass == "LX/3XE;" &&
                    reference.name == "A03" &&
                    reference.parameterTypes.size == 1
            ) {
                index
            } else {
                null
            }
        }
        require(buildCalls.size >= 4) {
            "Expected Story menu builder finalizations in A08"
        }

        // The first two and fourth finalizations feed organic Story overflow
        // menus. The third is the ad-only inner list and stays untouched. Add
        // after each result so branches targeting the finalization are covered.
        val menuDownloadInstructions = """
                invoke-static/range {p4 .. p4}, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->froggoCreateStoryDownload(Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;)LX/VyQ;
                move-result-object v0
                invoke-virtual {v4, v0}, Lcom/google/common/collect/ImmutableList${'$'}Builder;->add(Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList${'$'}Builder;
                invoke-static {v4}, LX/3XE;->A03(Lcom/google/common/collect/ImmutableList${'$'}Builder;)Lcom/google/common/collect/ImmutableList;
                move-result-object v8
                move-object/from16 v0, p4
            """.trimIndent()
        listOf(buildCalls[3], buildCalls[1], buildCalls[0]).forEach { index ->
            storyMoreMenu.method.addInstructions(index + 2, menuDownloadInstructions)
        }
    }
}
