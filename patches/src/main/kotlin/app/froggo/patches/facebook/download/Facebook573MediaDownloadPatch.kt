package app.froggo.patches.facebook.download

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable as toMutableField
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation

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

private val storyHeader = Fingerprint(
    returnType = "LX/3Pu;",
    parameters = listOf("LX/3QZ;"),
    custom = { method, classDef ->
        classDef.type == "LX/9Uw;" && method.name == "A1K"
    },
)

private val storyHeaderCallback = Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("LX/X6V;", "Ljava/lang/Object;"),
    custom = { method, classDef ->
        classDef.type == "LX/9Uw;" && method.name == "A1O"
    },
)

private val storyAlternateHeader = Fingerprint(
    returnType = "LX/3Pu;",
    parameters = listOf("LX/24H;"),
    custom = { method, classDef ->
        classDef.type == "LX/P0G;" && method.name == "render"
    },
)

private val fullscreenStoryTopbar = Fingerprint(
    returnType = "LX/3Pu;",
    parameters = listOf("LX/24H;"),
    custom = { method, classDef ->
        classDef.type == "LX/9W5;" && method.name == "render"
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

private fun compactStoryDownloadWorkerInstructions(
    imagePathPrefix: String,
    videoPathPrefix: String,
) = """
    move-object v10, p0
    iget-object v10, v10, LX/WKI;->A02:Ljava/lang/Object;
    check-cast v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;
    iget-object v0, v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A09:Landroid/content/Context;
    iget-object v1, v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A02:Lcom/facebook/stories/model/StoryCard;
    const-string v4, "FroggoPatches"
    const-string v5, "story-worker-start"
    invoke-static {v4, v5}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
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
    const-string v5, "$videoPathPrefix"
    goto :froggo_story_download_path_prefix_ready

    :froggo_story_download_images_collection
    sget-object v9, Landroid/provider/MediaStore${'$'}Images${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    const-string v5, "$imagePathPrefix"

    :froggo_story_download_path_prefix_ready
    new-instance v10, Ljava/lang/StringBuilder;
    invoke-direct {v10, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v10, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    if-eqz v8, :froggo_story_download_image_owner_folder
    const-string v5, "/Historias/"
    goto :froggo_story_download_owner_folder_ready
    :froggo_story_download_image_owner_folder
    const-string v5, "/"
    :froggo_story_download_owner_folder_ready
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
    check-cast v3, Landroid/net/Uri;
    move-object v9, v4
    check-cast v9, Ljava/lang/String;
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
    const/4 v15, 0x1
    invoke-static {v15}, LX/WKI;->froggoShowDownloadFeedbackResult(Z)V
    const/4 v15, 0x0
    invoke-static {v0, v15}, LX/WKI;->froggoPostLocalizedDownloadSuccess(Landroid/content/Context;Z)V
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
    const/4 v15, 0x0
    invoke-static {v15}, LX/WKI;->froggoShowDownloadFeedbackResult(Z)V
    const-string v4, "🐸 No se pudo descargar la Historia"
    invoke-static {v0, v4}, LX/WKI;->froggoPostToast(Landroid/content/Context;Ljava/lang/CharSequence;)V
    goto :froggo_story_download_finish

    :froggo_story_download_catch
    move-exception v5
    const-string v10, "FroggoPatches"
    const-string v4, "story download exception"
    invoke-static {v10, v4, v5}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    goto :froggo_story_download_fail
""".trimIndent()

private fun fullscreenStoryDownloadWorkerInstructions(
    imagePathPrefix: String,
    videoPathPrefix: String,
) = run {
    val callbackHeader = """
        move-object v10, p0
        iget-object v10, v10, LX/WKI;->A02:Ljava/lang/Object;
        check-cast v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;
        iget-object v0, v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A09:Landroid/content/Context;
        iget-object v1, v10, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A02:Lcom/facebook/stories/model/StoryCard;
    """.trimIndent()
    compactStoryDownloadWorkerInstructions(imagePathPrefix, videoPathPrefix)
        .replace(
            callbackHeader,
            """
                move-object v0, p0
                iget-object v0, v0, LX/WKI;->A00:Ljava/lang/Object;
                check-cast v0, Landroid/content/Context;
                move-object v1, p0
                iget-object v1, v1, LX/WKI;->A01:Ljava/lang/Object;
                check-cast v1, Lcom/facebook/stories/model/StoryCard;
            """.trimIndent(),
        )
        .replace("froggo_story_download_", "froggo_fullscreen_story_download_")
}

private fun storyFirstFrameWorkerInstructions(imagePathPrefix: String) = """
    iget v9, p0, LX/WKI;->${'$'}t:I
    const/16 v10, 0x89
    if-eq v9, v10, :froggo_story_frame_fullscreen
    iget-object v0, p0, LX/WKI;->A00:Ljava/lang/Object;
    check-cast v0, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;
    iget-object v1, v0, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A09:Landroid/content/Context;
    iget-object v2, v0, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A02:Lcom/facebook/stories/model/StoryCard;
    goto :froggo_story_frame_payload_ready
    :froggo_story_frame_fullscreen
    iget-object v1, p0, LX/WKI;->A00:Ljava/lang/Object;
    check-cast v1, Landroid/content/Context;
    iget-object v2, p0, LX/WKI;->A01:Ljava/lang/Object;
    check-cast v2, Lcom/facebook/stories/model/StoryCard;
    :froggo_story_frame_payload_ready
    const/4 v3, 0x0
    :froggo_story_frame_try
    invoke-virtual {v2}, Lcom/facebook/stories/model/StoryCard;->getMedia()LX/9Uo;
    move-result-object v4
    if-eqz v4, :froggo_story_frame_fail
    iget-object v4, v4, LX/9Uo;->A05:Ljava/lang/String;
    if-eqz v4, :froggo_story_frame_fail
    new-instance v5, Landroid/media/MediaMetadataRetriever;
    invoke-direct {v5}, Landroid/media/MediaMetadataRetriever;-><init>()V
    new-instance v6, Ljava/util/HashMap;
    invoke-direct {v6}, Ljava/util/HashMap;-><init>()V
    const-string v7, "User-Agent"
    const-string v8, "Mozilla/5.0"
    invoke-virtual {v6, v7, v8}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    invoke-virtual {v5, v4, v6}, Landroid/media/MediaMetadataRetriever;->setDataSource(Ljava/lang/String;Ljava/util/Map;)V
    const-wide/16 v6, 0x0
    invoke-virtual {v5, v6, v7}, Landroid/media/MediaMetadataRetriever;->getFrameAtTime(J)Landroid/graphics/Bitmap;
    move-result-object v4
    invoke-virtual {v5}, Landroid/media/MediaMetadataRetriever;->release()V
    if-eqz v4, :froggo_story_frame_fail

    invoke-virtual {v2}, Lcom/facebook/stories/model/StoryCard;->A0U()LX/CPA;
    move-result-object v5
    if-eqz v5, :froggo_story_frame_unknown
    invoke-interface {v5}, LX/CPA;->C2z()Ljava/lang/String;
    move-result-object v5
    if-nez v5, :froggo_story_frame_owner_ready
    :froggo_story_frame_unknown
    const-string v5, "unknown"
    :froggo_story_frame_owner_ready
    const-string v6, "[^A-Za-z0-9._-]"
    const-string v7, "_"
    invoke-virtual {v5, v6, v7}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v5
    new-instance v6, Ljava/lang/StringBuilder;
    const-string v7, "$imagePathPrefix"
    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v5, "/"
    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v5
    new-instance v6, Ljava/text/SimpleDateFormat;
    const-string v7, "yyyyMMdd_HHmmss'_story-01.jpg'"
    sget-object v8, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-direct {v6, v7, v8}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;Ljava/util/Locale;)V
    new-instance v7, Ljava/util/Date;
    invoke-direct {v7}, Ljava/util/Date;-><init>()V
    invoke-virtual {v6, v7}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;
    move-result-object v6
    new-instance v7, Landroid/content/ContentValues;
    invoke-direct {v7}, Landroid/content/ContentValues;-><init>()V
    const-string v8, "_display_name"
    invoke-virtual {v7, v8, v6}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v6, "mime_type"
    const-string v8, "image/jpeg"
    invoke-virtual {v7, v6, v8}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v6, "relative_path"
    invoke-virtual {v7, v6, v5}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/String;)V
    const-string v6, "is_pending"
    const/4 v8, 0x1
    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v8
    invoke-virtual {v7, v6, v8}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    invoke-virtual {v1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;
    move-result-object v5
    sget-object v6, Landroid/provider/MediaStore${'$'}Images${'$'}Media;->EXTERNAL_CONTENT_URI:Landroid/net/Uri;
    invoke-virtual {v5, v6, v7}, Landroid/content/ContentResolver;->insert(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;
    move-result-object v3
    if-eqz v3, :froggo_story_frame_fail
    invoke-virtual {v5, v3}, Landroid/content/ContentResolver;->openOutputStream(Landroid/net/Uri;)Ljava/io/OutputStream;
    move-result-object v6
    if-eqz v6, :froggo_story_frame_fail
    sget-object v7, Landroid/graphics/Bitmap${'$'}CompressFormat;->JPEG:Landroid/graphics/Bitmap${'$'}CompressFormat;
    const/16 v8, 0x63
    invoke-virtual {v4, v7, v8, v6}, Landroid/graphics/Bitmap;->compress(Landroid/graphics/Bitmap${'$'}CompressFormat;ILjava/io/OutputStream;)Z
    move-result v7
    invoke-virtual {v6}, Ljava/io/OutputStream;->close()V
    invoke-virtual {v4}, Landroid/graphics/Bitmap;->recycle()V
    if-eqz v7, :froggo_story_frame_fail
    new-instance v7, Landroid/content/ContentValues;
    invoke-direct {v7}, Landroid/content/ContentValues;-><init>()V
    const-string v6, "is_pending"
    const/4 v8, 0x0
    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v8
    invoke-virtual {v7, v6, v8}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
    const/4 v6, 0x0
    invoke-virtual {v5, v3, v7, v6, v6}, Landroid/content/ContentResolver;->update(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
    const/4 v6, 0x1
    invoke-static {v6}, LX/WKI;->froggoShowDownloadFeedbackResult(Z)V
    const/4 v6, 0x0
    invoke-static {v1, v6}, LX/WKI;->froggoPostLocalizedDownloadSuccess(Landroid/content/Context;Z)V
    return-void

    :froggo_story_frame_fail
    if-eqz v3, :froggo_story_frame_fail_feedback
    invoke-virtual {v1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;
    move-result-object v5
    const/4 v6, 0x0
    invoke-virtual {v5, v3, v6, v6}, Landroid/content/ContentResolver;->delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I
    :froggo_story_frame_fail_feedback
    const/4 v6, 0x0
    invoke-static {v6}, LX/WKI;->froggoShowDownloadFeedbackResult(Z)V
    const-string v6, "🐸 No se pudo guardar el primer frame"
    invoke-static {v1, v6}, LX/WKI;->froggoPostToast(Landroid/content/Context;Ljava/lang/CharSequence;)V
    return-void
    .catch Ljava/lang/Throwable; {:froggo_story_frame_try .. :froggo_story_frame_fail_feedback} :froggo_story_frame_catch
    :froggo_story_frame_catch
    move-exception v4
    const-string v5, "FroggoPatches"
    const-string v6, "story first-frame exception"
    invoke-static {v5, v6, v4}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    goto :froggo_story_frame_fail
""".trimIndent()

private val compactVideoDownloadWorkerInstructions = """
    move-object v1, p0
    iget-object v1, v1, LX/bq4;->A01:LX/b1P;
    invoke-virtual {v1}, Landroid/view/View;->getContext()Landroid/content/Context;
    move-result-object v0
    const-string v10, "FroggoPatches"
    const-string v11, "worker-start"
    invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    const-string v12, "🐸 Descarga iniciada"
    invoke-static {v0, v12}, LX/WKI;->froggoPostToast(Landroid/content/Context;Ljava/lang/CharSequence;)V
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
    const-string v12, "🐸 Descarga finalizada"
    invoke-static {v0, v12}, LX/WKI;->froggoPostToast(Landroid/content/Context;Ljava/lang/CharSequence;)V
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

private fun compactReelDownloadWorkerInstructions(videoPathPrefix: String) = """
    move-object v1, p0
    iget-object v1, v1, LX/WKI;->A00:Ljava/lang/Object;
    check-cast v1, LX/3QZ;
    iget-object v0, v1, LX/3QZ;->A0C:Landroid/content/Context;
    const-string v1, "FroggoPatches"
    const-string v2, "reel-dm-start"
    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    :froggo_reel_dm_try_start
    move-object v1, p0
    iget-object v1, v1, LX/WKI;->A01:Ljava/lang/Object;
    check-cast v1, LX/4ta;
    iget-object v2, v1, LX/4ta;->A03:Lcom/facebook/video/engine/api/VideoPlayerParams;
    if-eqz v2, :froggo_reel_dm_fail
    iget-object v3, v2, Lcom/facebook/video/engine/api/VideoPlayerParams;->A0b:Lcom/facebook/video/engine/api/VideoDataSource;
    if-eqz v3, :froggo_reel_dm_fail
    iget-object v4, v3, Lcom/facebook/video/engine/api/VideoDataSource;->A08:Landroid/net/Uri;
    if-eqz v4, :froggo_reel_dm_fail

    invoke-virtual {v4}, Landroid/net/Uri;->getScheme()Ljava/lang/String;
    move-result-object v5
    if-eqz v5, :froggo_reel_dm_fail
    const-string v6, "http"
    invoke-virtual {v5, v6}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
    move-result v7
    if-nez v7, :froggo_reel_dm_url_ok
    const-string v6, "https"
    invoke-virtual {v5, v6}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
    move-result v7
    if-eqz v7, :froggo_reel_dm_fail

    :froggo_reel_dm_url_ok
    new-instance v5, Landroid/app/DownloadManager${'$'}Request;
    invoke-direct {v5, v4}, Landroid/app/DownloadManager${'$'}Request;-><init>(Landroid/net/Uri;)V
    const-string v6, "User-Agent"
    const-string v7, "Mozilla/5.0"
    invoke-virtual {v5, v6, v7}, Landroid/app/DownloadManager${'$'}Request;->addRequestHeader(Ljava/lang/String;Ljava/lang/String;)Landroid/app/DownloadManager${'$'}Request;
    const-string v6, "🐸 Reel"
    invoke-virtual {v5, v6}, Landroid/app/DownloadManager${'$'}Request;->setTitle(Ljava/lang/CharSequence;)Landroid/app/DownloadManager${'$'}Request;
    const-string v6, "video/mp4"
    invoke-virtual {v5, v6}, Landroid/app/DownloadManager${'$'}Request;->setMimeType(Ljava/lang/String;)Landroid/app/DownloadManager${'$'}Request;
    const/4 v6, 0x2
    invoke-virtual {v5, v6}, Landroid/app/DownloadManager${'$'}Request;->setNotificationVisibility(I)Landroid/app/DownloadManager${'$'}Request;

    move-object v6, p0
    iget-object v6, v6, LX/WKI;->A02:Ljava/lang/Object;
    check-cast v6, LX/BsO;
    invoke-static {v6}, LX/2wL;->A0F(LX/BsO;)Ljava/lang/String;
    move-result-object v6
    if-nez v6, :froggo_reel_owner_ready
    const-string v6, "unknown"
    :froggo_reel_owner_ready
    const-string v7, "@"
    invoke-virtual {v6, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
    move-result v7
    if-eqz v7, :froggo_reel_owner_no_at
    const/4 v7, 0x1
    invoke-virtual {v6, v7}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v6
    :froggo_reel_owner_no_at
    const-string v7, "[^A-Za-z0-9._-]"
    const-string v8, "_"
    invoke-virtual {v6, v7, v8}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v6
    new-instance v7, Ljava/lang/StringBuilder;
    const-string v8, "$videoPathPrefix"
    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v7, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v6, "/Reels/"
    invoke-virtual {v7, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    new-instance v6, Ljava/text/SimpleDateFormat;
    const-string v8, "yyyyMMdd_HHmmss'_reel-01.mp4'"
    sget-object v9, Ljava/util/Locale;->US:Ljava/util/Locale;
    invoke-direct {v6, v8, v9}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;Ljava/util/Locale;)V
    new-instance v8, Ljava/util/Date;
    invoke-direct {v8}, Ljava/util/Date;-><init>()V
    invoke-virtual {v6, v8}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;
    move-result-object v6
    invoke-virtual {v7, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v6
    sget-object v7, Landroid/os/Environment;->DIRECTORY_MOVIES:Ljava/lang/String;
    invoke-virtual {v5, v7, v6}, Landroid/app/DownloadManager${'$'}Request;->setDestinationInExternalPublicDir(Ljava/lang/String;Ljava/lang/String;)Landroid/app/DownloadManager${'$'}Request;

    const-string v6, "download"
    invoke-virtual {v0, v6}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v6
    check-cast v6, Landroid/app/DownloadManager;
    invoke-virtual {v6, v5}, Landroid/app/DownloadManager;->enqueue(Landroid/app/DownloadManager${'$'}Request;)J
    move-result-wide v7
    const-string v1, "FroggoPatches"
    const-string v2, "reel-dm-enqueued"
    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    const/4 v9, 0x0
    :froggo_reel_dm_poll
    const/16 v10, 0x12c
    if-ge v9, v10, :froggo_reel_dm_timeout
    new-instance v10, Landroid/app/DownloadManager${'$'}Query;
    invoke-direct {v10}, Landroid/app/DownloadManager${'$'}Query;-><init>()V
    const/4 v11, 0x1
    new-array v11, v11, [J
    const/4 v12, 0x0
    aput-wide v7, v11, v12
    invoke-virtual {v10, v11}, Landroid/app/DownloadManager${'$'}Query;->setFilterById([J)Landroid/app/DownloadManager${'$'}Query;
    invoke-virtual {v6, v10}, Landroid/app/DownloadManager;->query(Landroid/app/DownloadManager${'$'}Query;)Landroid/database/Cursor;
    move-result-object v10
    if-eqz v10, :froggo_reel_dm_poll_sleep
    invoke-interface {v10}, Landroid/database/Cursor;->moveToFirst()Z
    move-result v11
    if-eqz v11, :froggo_reel_dm_poll_close
    const-string v11, "status"
    invoke-interface {v10, v11}, Landroid/database/Cursor;->getColumnIndex(Ljava/lang/String;)I
    move-result v11
    if-ltz v11, :froggo_reel_dm_poll_close
    invoke-interface {v10, v11}, Landroid/database/Cursor;->getInt(I)I
    move-result v11
    invoke-interface {v10}, Landroid/database/Cursor;->close()V
    const/16 v12, 0x8
    if-eq v11, v12, :froggo_reel_dm_success
    const/16 v12, 0x10
    if-eq v11, v12, :froggo_reel_dm_fail
    goto :froggo_reel_dm_poll_sleep

    :froggo_reel_dm_poll_close
    invoke-interface {v10}, Landroid/database/Cursor;->close()V

    :froggo_reel_dm_poll_sleep
    const-wide/16 v10, 0x64
    invoke-static {v10, v11}, Landroid/os/SystemClock;->sleep(J)V
    add-int/lit8 v9, v9, 0x1
    goto :froggo_reel_dm_poll

    :froggo_reel_dm_success
    const/4 v15, 0x1
    invoke-static {v15}, LX/WKI;->froggoShowDownloadFeedbackResult(Z)V
    const/4 v15, 0x1
    invoke-static {v0, v15}, LX/WKI;->froggoPostLocalizedDownloadSuccess(Landroid/content/Context;Z)V
    goto :froggo_reel_dm_finish

    :froggo_reel_dm_timeout
    const-string v1, "FroggoPatches"
    const-string v2, "reel DownloadManager timeout"
    invoke-static {v1, v2}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    goto :froggo_reel_dm_fail

    :froggo_reel_dm_fail
    const-string v1, "FroggoPatches"
    const-string v2, "reel DownloadManager failed"
    invoke-static {v1, v2}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    const/4 v15, 0x0
    invoke-static {v15}, LX/WKI;->froggoShowDownloadFeedbackResult(Z)V
    const-string v1, "🐸 No se pudo descargar el Reel"
    invoke-static {v0, v1}, LX/WKI;->froggoPostToast(Landroid/content/Context;Ljava/lang/CharSequence;)V

    :froggo_reel_dm_finish
    return-void
    .catch Ljava/lang/Throwable; {:froggo_reel_dm_try_start .. :froggo_reel_dm_finish} :froggo_reel_dm_catch

    :froggo_reel_dm_catch
    move-exception v1
    const-string v2, "FroggoPatches"
    const-string v3, "reel DownloadManager exception"
    invoke-static {v2, v3, v1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    const/4 v15, 0x0
    invoke-static {v15}, LX/WKI;->froggoShowDownloadFeedbackResult(Z)V
    const-string v1, "🐸 No se pudo descargar el Reel"
    invoke-static {v0, v1}, LX/WKI;->froggoPostToast(Landroid/content/Context;Ljava/lang/CharSequence;)V
    return-void
""".trimIndent()

@Suppress("unused")
val downloadFacebookMedia573Patch = bytecodePatch(
    name = "Download Facebook Media (573)",
    description = "Adds direct downloads for the visible Story, Reel, and video media through MediaStore.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    val imageFolderOption = stringOption(
        key = "facebookImageFolder",
        default = "Pictures/FroggoPatches/Facebook/@",
        values = mapOf("Pictures/FroggoPatches/Facebook/@" to "Pictures/FroggoPatches/Facebook/@"),
        title = "Facebook image folder",
        description = "Relative MediaStore folder prefix. The creator name is appended after @.",
        required = true,
    ) { it != null && it.startsWith("Pictures/") && it.endsWith("@") && ".." !in it }

    val videoFolderOption = stringOption(
        key = "facebookVideoFolder",
        default = "FroggoPatches/Facebook/@",
        values = mapOf("Movies/FroggoPatches/Facebook/@" to "FroggoPatches/Facebook/@"),
        title = "Facebook video folder",
        description = "Folder below Movies. The creator name is appended after @.",
        required = true,
    ) { it != null && !it.startsWith("/") && it.endsWith("@") && ".." !in it }

    execute {
        val imagePathPrefix = imageFolderOption.value!!
        val videoPathPrefix = "Movies/${videoFolderOption.value!!}"
        val storyDirectActionHash = -2013570421
        val storyDirectButtonHelper = ImmutableMethod(
            storyHeader.classDef.type,
            "froggoCreateStoryDownloadButton",
            listOf(
                ImmutableMethodParameter("LX/3QZ;", null, null),
                ImmutableMethodParameter("Z", null, null),
            ),
            "LX/4hG;",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    invoke-static {p0}, LX/4hG;->A00(LX/3QZ;)LX/4hH;
                    move-result-object v0
                    const-string v6, "FroggoPatches"
                    const-string v7, "story-button-built"
                    invoke-static {v6, v7}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    const/high16 v1, 0x41c00000
                    invoke-virtual {v0, v1}, LX/4hH;->A1j(F)V
                    const/high16 v1, 0x41c00000
                    invoke-virtual {v0, v1}, LX/4hH;->A1X(F)V
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1}, LX/4hH;->A1W(F)V
                    invoke-static {v0}, LX/9Di;->A1X(LX/Nqn;)V
                    sget-object v1, Lcom/facebook/fds/core/theme/component/FDSColors;->A00:Lcom/facebook/fds/core/theme/component/FDSColors;
                    if-eqz p1, :froggo_story_download_button_light
                    sget-object v2, LX/1y5;->A09:LX/1y5;
                    goto :froggo_story_download_button_color_ready
                    :froggo_story_download_button_light
                    sget-object v2, LX/1y5;->A4C:LX/1y5;
                    :froggo_story_download_button_color_ready
                    invoke-virtual {v1, v2, p0}, Lcom/facebook/fds/core/theme/component/FDSColors;->A02(LX/1y5;LX/3QZ;)I
                    move-result v1
                    invoke-virtual {v0, v1}, LX/4hH;->A32(I)V
                    sget v1, Lcom/facebook/katana/R${'$'}drawable${'$'}3;->fb_ic_download_24:I
                    invoke-virtual {v0, v1}, LX/4hH;->A35(I)V
                    move-object v6, v0
                    move-object v0, p0
                    sget-object v1, LX/1K7;->A03:LX/1K7;
                    const-class v2, ${storyHeader.classDef.type}
                    const-string v3, "ExpandedMediaFeedHeaderComponent"
                    const/4 v4, 0x0
                    const v5, $storyDirectActionHash
                    invoke-virtual/range {v0 .. v5}, LX/3QZ;->A07(LX/1K7;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;I)LX/X6V;
                    move-result-object v1
                    move-object v0, v6
                    invoke-virtual {v0, v1}, LX/4hH;->A2C(LX/X6V;)V
                    sget-object v1, LX/2PU;->A04:LX/2PU;
                    invoke-virtual {v0, v1}, LX/Nqn;->A2T(LX/2PU;)V
                    invoke-virtual {v0}, LX/Nqn;->A1O()V
                    iget-object v0, v0, LX/4hH;->A00:LX/4hG;
                    return-object v0
                """.trimIndent(),
            )
        }
        storyHeader.classDef.methods.add(storyDirectButtonHelper)

        storyHeaderCallback.method.addInstructions(
            0,
            """
                move-object/from16 v4, p1
                move-object/from16 v5, p2
                iget v0, v4, LX/X6V;->A01:I
                const v1, $storyDirectActionHash
                if-ne v0, v1, :froggo_story_header_stock_callback
                instance-of v1, v5, LX/1MP;
                if-eqz v1, :froggo_story_header_touch_done
                check-cast v5, LX/1MP;
                invoke-virtual {v5}, LX/1MP;->A00()Landroid/view/View;
                move-result-object v5
                :froggo_story_header_start
                const-string v2, "FroggoPatches"
                const-string v3, "story-action-callback"
                invoke-static {v2, v3}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                iget-object v0, v4, LX/X6V;->A00:LX/1K9;
                iget-object v0, v0, LX/1K9;->A00:LX/3QZ;
                invoke-static {v0}, LX/41t;->A0O(LX/3QZ;)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, LX/9VC;
                iget-object v0, v0, LX/9VC;->A00:Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;
                invoke-static {v0, v5}, LX/WKI;->froggoChooseStoryDownload(Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;Landroid/view/View;)V
                const/4 v0, 0x0
                return-object v0
                :froggo_story_header_touch_done
                sget-object v0, LX/0FI;->A00:LX/0FI;
                return-object v0
                :froggo_story_header_stock_callback
            """.trimIndent(),
        )

        val storyHeaderInstructions = storyHeader.method.implementation!!.instructions
        val storyDotsCalls = storyHeaderInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                reference?.definingClass == storyHeader.classDef.type &&
                    reference.name == "A00" &&
                    reference.parameterTypes == listOf("LX/3QZ;", "Z")
            ) {
                index
            } else {
                null
            }
        }
        val storyDotsCallSites = storyDotsCalls.mapNotNull { callIndex ->
            val dotsResultRegister =
                (storyHeaderInstructions.getOrNull(callIndex + 1) as? OneRegisterInstruction)?.registerA
                    ?: return@mapNotNull null
            fun isDotsCollectionAdd(index: Int): Boolean {
                val instruction = storyHeaderInstructions[index]
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                return reference?.definingClass == "Ljava/util/AbstractCollection;" &&
                    reference.name == "add" &&
                    reference.parameterTypes == listOf("Ljava/lang/Object;") &&
                    (instruction as? FiveRegisterInstruction)?.registerCount == 2 &&
                    (instruction as FiveRegisterInstruction).registerD == dotsResultRegister
            }
            val addIndex =
            (callIndex + 1 until minOf(callIndex + 96, storyHeaderInstructions.size))
                    .firstOrNull(::isDotsCollectionAdd)
                    ?: (0 until callIndex).reversed().firstOrNull(::isDotsCollectionAdd)
            if (addIndex == null) null else callIndex to addIndex
        }
        require(storyDotsCallSites.size == 2) {
            "Expected both Story header dots layouts to add their button to a collection"
        }
        val directButtonReference = ImmutableMethodReference(
            storyHeader.classDef.type,
            "froggoCreateStoryDownloadButton",
            listOf("LX/3QZ;", "Z"),
            "LX/4hG;",
        )
        storyDotsCallSites.asReversed().forEach { (storyDotsCall, dotsAddIndex) ->
            val dotsCallRegisters = storyHeaderInstructions[storyDotsCall] as FiveRegisterInstruction
            require(dotsCallRegisters.registerCount == 2) {
                "Expected the Story header dots builder to receive C3QZ and theme boolean"
            }
            val dotsResultRegister =
                (storyHeaderInstructions[storyDotsCall + 1] as OneRegisterInstruction).registerA
            val dotsAddRegisters = storyHeaderInstructions[dotsAddIndex] as FiveRegisterInstruction
            val storyHeaderAddReference =
                (storyHeaderInstructions[dotsAddIndex] as ReferenceInstruction).reference
            storyHeader.method.addInstructions(
                dotsAddIndex + 1,
                listOf(
                    BuilderInstruction35c(
                        Opcode.INVOKE_STATIC,
                        dotsCallRegisters.registerCount,
                        dotsCallRegisters.registerC,
                        dotsCallRegisters.registerD,
                        0,
                        0,
                        0,
                        directButtonReference,
                    ),
                    BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, dotsResultRegister),
                    BuilderInstruction35c(
                        Opcode.INVOKE_VIRTUAL,
                        2,
                        dotsAddRegisters.registerC,
                        dotsResultRegister,
                        0,
                        0,
                        0,
                        storyHeaderAddReference,
                    ),
                ),
            )
        }

        val storyAlternateHeaderInstructions = storyAlternateHeader.method.implementation!!.instructions
        val storyAlternateHeaderAnchors = storyAlternateHeaderInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                    reference?.definingClass == "LX/Nqo;" &&
                    reference.name == "A0x"
            ) {
                index
            } else {
                null
            }
        }
        require(storyAlternateHeaderAnchors.size == 1) {
            "Expected one alternate Story header child collection anchor"
        }
        storyAlternateHeader.method.addInstructions(
            storyAlternateHeaderAnchors.single(),
            """
                invoke-static {v6}, LX/4hG;->A00(LX/3QZ;)LX/4hH;
                move-result-object v0
                const/high16 v1, 0x41c00000
                invoke-virtual {v0, v1}, LX/4hH;->A1j(F)V
                const/high16 v1, 0x41c00000
                invoke-virtual {v0, v1}, LX/4hH;->A1X(F)V
                const/4 v1, 0x0
                invoke-virtual {v0, v1}, LX/4hH;->A1W(F)V
                invoke-static {v0}, LX/9Di;->A1X(LX/Nqn;)V
                sget-object v1, LX/1y5;->A4C:LX/1y5;
                sget-object v2, Lcom/facebook/fds/core/theme/component/FDSColors;->A00:Lcom/facebook/fds/core/theme/component/FDSColors;
                invoke-static {v0, v1, v2, v6}, LX/HrH;->A1D(LX/4hH;LX/1y5;Lcom/facebook/fds/core/theme/component/FDSColors;LX/3QZ;)V
                sget v1, Lcom/facebook/katana/R${'$'}drawable${'$'}3;->fb_ic_download_24:I
                invoke-virtual {v0, v1}, LX/4hH;->A35(I)V
                invoke-static {v7}, LX/CQu;->A10(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v1
                new-instance v2, LX/WKI;
                const/16 v3, 0x7f
                move-object v9, v1
                move-object v10, v1
                move-object v11, v1
                invoke-direct {v2, v3, v9, v10, v11}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                new-instance v1, LX/3S5;
                const/4 v3, 0x0
                invoke-direct {v1, v3, v2}, LX/3S5;-><init>(LX/3QZ;Lkotlin/jvm/functions/Function1;)V
                invoke-virtual {v0, v1}, LX/4hH;->A2C(LX/X6V;)V
                invoke-virtual {v0}, LX/Nqn;->A1O()V
                iget-object v0, v0, LX/4hH;->A00:LX/4hG;
                invoke-virtual {v4, v0}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z
            """.trimIndent(),
        )

        val fullscreenStoryDownloadHelper = ImmutableMethod(
            fullscreenStoryTopbar.classDef.type,
            "froggoCreateFullscreenStoryDownloadButton",
            listOf(
                ImmutableMethodParameter(fullscreenStoryTopbar.classDef.type, null, null),
                ImmutableMethodParameter("LX/3QZ;", null, null),
            ),
            "LX/AnR;",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    invoke-static {p1}, LX/9Di;->A0o(LX/3QZ;)Ljava/lang/Object;
                    move-result-object v0
                    check-cast v0, LX/BsZ;
                    invoke-static {v0}, LX/BsZ;->A00(LX/BsZ;)Lcom/facebook/auth/usersession/FbUserSession;
                    move-result-object v1
                    invoke-virtual {p1}, LX/3QZ;->A01()Landroid/content/Context;
                    move-result-object v2
                    move-object/from16 v3, p0
                    iget-object v3, v3, LX/9W5;->A04:Lcom/facebook/stories/model/StoryCard;

                    new-instance v4, LX/WKI;
                    const/16 v5, 0x83
                    move-object v6, v2
                    move-object v7, v3
                    const/4 v8, 0x0
                    invoke-direct {v4, v5, v6, v7, v8}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V

                    invoke-static {v4}, LX/3S5;->A00(Lkotlin/jvm/functions/Function1;)LX/3S5;
                    move-result-object v5

                    sget-object v2, LX/1Vq;->A80:LX/1Vq;
                    sget-object v3, LX/1c6;->A02:LX/1c6;
                    move-object v4, v5
                    const-string v5, "Descargar"
                    const-string v6, "storyviewer_download_button"
                    new-instance v0, LX/AnR;
                    invoke-direct/range {v0 .. v6}, LX/AnR;-><init>(Lcom/facebook/auth/usersession/FbUserSession;LX/1Vq;LX/1c6;LX/X6V;Ljava/lang/String;Ljava/lang/String;)V
                    return-object v0
                """.trimIndent(),
            )
        }
        fullscreenStoryTopbar.classDef.methods.add(fullscreenStoryDownloadHelper)

        val fullscreenStoryTopbarInstructions = fullscreenStoryTopbar.method.implementation!!.instructions
        val storyTrayConstructorCalls = fullscreenStoryTopbarInstructions.withIndex().mapNotNull { (index, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                reference?.definingClass == "LX/An8;" &&
                    reference.name == "<init>" &&
                    reference.parameterTypes == listOf(
                        "Lcom/facebook/auth/usersession/FbUserSession;",
                        "LX/FlR;",
                        "Lcom/facebook/stories/model/StoryBucket;",
                        "LX/BsZ;",
                    )
            ) {
                index
            } else {
                null
            }
        }
        require(storyTrayConstructorCalls.size == 1) {
            "Expected one fullscreen Story tray entrypoint component"
        }
        val storyTrayConstructorIndex = storyTrayConstructorCalls.single()
        val storyTrayConstructorRegisters = fullscreenStoryTopbarInstructions[storyTrayConstructorIndex] as FiveRegisterInstruction
        val storyTrayRegister = storyTrayConstructorRegisters.registerC
        val storyTrayAddIndex =
            (storyTrayConstructorIndex + 1 until minOf(storyTrayConstructorIndex + 5, fullscreenStoryTopbarInstructions.size))
                .firstOrNull { index ->
                    val instruction = fullscreenStoryTopbarInstructions[index]
                    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    reference?.definingClass == "Ljava/util/AbstractCollection;" &&
                        reference.name == "add" &&
                        reference.parameterTypes == listOf("Ljava/lang/Object;") &&
                        (instruction as? FiveRegisterInstruction)?.registerCount == 2 &&
                        (instruction as FiveRegisterInstruction).registerD == storyTrayRegister
                }
        requireNotNull(storyTrayAddIndex) {
            "Expected the fullscreen Story tray entrypoint to be added to its topbar collection"
        }
        fullscreenStoryTopbar.method.addInstructions(
            storyTrayAddIndex + 1,
            """
                move-object/from16 v66, p0
                move-object/from16 v67, v44
                invoke-static/range {v66 .. v67}, LX/9W5;->froggoCreateFullscreenStoryDownloadButton(LX/9W5;LX/3QZ;)LX/AnR;
                move-result-object v8
                invoke-virtual {v9, v8}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z
            """.trimIndent(),
        )

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
        callbackClass.interfaces.removeAll { it == "Ljava/lang/Runnable;" }
        callbackClass.interfaces.add("Ljava/lang/Runnable;")
        callbackClass.interfaces.removeAll { it == "Lkotlin/jvm/functions/Function1;" }
        callbackClass.interfaces.add("Lkotlin/jvm/functions/Function1;")
        callbackClass.interfaces.removeAll { it == "Landroid/content/DialogInterface${'$'}OnClickListener;" }
        callbackClass.interfaces.add("Landroid/content/DialogInterface${'$'}OnClickListener;")
        callbackClass.fields.add(
            ImmutableField(
                callbackClass.type,
                "froggoDownloadButton",
                "Landroid/view/View;",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                emptySet(),
                emptySet(),
            ).toMutableField(),
        )
        callbackClass.fields.add(
            ImmutableField(
                callbackClass.type,
                "froggoDownloadButtonTint",
                "Landroid/content/res/ColorStateList;",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                emptySet(),
                emptySet(),
            ).toMutableField(),
        )
        callbackClass.fields.add(
            ImmutableField(
                callbackClass.type,
                "froggoDownloadSpinner",
                "Landroid/widget/ProgressBar;",
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                emptySet(),
                emptySet(),
            ).toMutableField(),
        )
        val toastHelper = ImmutableMethod(
            callbackClass.type,
            "froggoPostToast",
            listOf(
                ImmutableMethodParameter("Landroid/content/Context;", null, null),
                ImmutableMethodParameter("Ljava/lang/CharSequence;", null, null),
            ),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(8),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    new-instance v0, Landroid/os/Handler;
                    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                    move-result-object v1
                    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                    new-instance v1, LX/WKI;
                    const/16 v2, 0x82
                    move-object v3, p0
                    move-object v4, p1
                    const/4 v5, 0x0
                    invoke-direct {v1, v2, v3, v4, v5}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(toastHelper)

        val localizedSuccessToastHelper = ImmutableMethod(
            callbackClass.type,
            "froggoPostLocalizedDownloadSuccess",
            listOf(
                ImmutableMethodParameter("Landroid/content/Context;", null, null),
                ImmutableMethodParameter("Z", null, null),
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
                    invoke-static {}, Ljava/util/Locale;->getDefault()Ljava/util/Locale;
                    move-result-object v0
                    invoke-virtual {v0}, Ljava/util/Locale;->getLanguage()Ljava/lang/String;
                    move-result-object v0
                    const-string v1, "es"
                    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :froggo_success_english
                    if-eqz p1, :froggo_success_spanish_story
                    const-string v1, "Reel descargado"
                    goto :froggo_success_ready
                    :froggo_success_spanish_story
                    const-string v1, "Historia descargada"
                    goto :froggo_success_ready
                    :froggo_success_english
                    if-eqz p1, :froggo_success_english_story
                    const-string v1, "Reel downloaded"
                    goto :froggo_success_ready
                    :froggo_success_english_story
                    const-string v1, "Story downloaded"
                    :froggo_success_ready
                    invoke-static {p0, v1}, LX/WKI;->froggoPostToast(Landroid/content/Context;Ljava/lang/CharSequence;)V
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(localizedSuccessToastHelper)

        val captureDownloadButtonHelper = ImmutableMethod(
            callbackClass.type,
            "froggoCaptureDownloadButton",
            listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(8),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    :froggo_capture_button_try
                    move-object v0, p0
                    instance-of v1, v0, Landroid/view/View;
                    if-nez v1, :froggo_capture_button_direct
                    instance-of v1, v0, LX/1MP;
                    if-nez v1, :froggo_capture_button_click
                    instance-of v1, v0, LX/43B;
                    if-nez v1, :froggo_capture_button_pressed
                    instance-of v1, v0, LX/1LV;
                    if-nez v1, :froggo_capture_button_touch
                    goto :froggo_capture_button_done

                    :froggo_capture_button_direct
                    check-cast v0, Landroid/view/View;
                    goto :froggo_capture_button_store

                    :froggo_capture_button_click
                    check-cast v0, LX/1MP;
                    invoke-virtual {v0}, LX/1MP;->A00()Landroid/view/View;
                    move-result-object v0
                    goto :froggo_capture_button_store

                    :froggo_capture_button_pressed
                    check-cast v0, LX/43B;
                    invoke-virtual {v0}, LX/43B;->A00()Landroid/view/View;
                    move-result-object v0
                    goto :froggo_capture_button_store

                    :froggo_capture_button_touch
                    check-cast v0, LX/1LV;
                    invoke-virtual {v0}, LX/1LV;->A01()Landroid/view/View;
                    move-result-object v0

                    :froggo_capture_button_store
                    if-eqz v0, :froggo_capture_button_done
                    sput-object v0, LX/WKI;->froggoDownloadButton:Landroid/view/View;
                    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
                    move-result-object v1
                    invoke-virtual {v1}, Ljava/lang/Class;->getName()Ljava/lang/String;
                    move-result-object v2
                    const-string v1, "FroggoButtonView"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    const-string v1, "FroggoPatches"
                    const-string v2, "feedback-button-captured"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                    :froggo_capture_button_done
                    return-void
                    .catch Ljava/lang/Throwable; {:froggo_capture_button_try .. :froggo_capture_button_done} :froggo_capture_button_catch
                    :froggo_capture_button_catch
                    move-exception v0
                    const-string v1, "FroggoPatches"
                    const-string v2, "feedback-capture-exception"
                    invoke-static {v1, v2, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(captureDownloadButtonHelper)

        val animationStartHelper = ImmutableMethod(
            callbackClass.type,
            "froggoShowDownloadFeedbackStart",
            listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(10),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    :froggo_feedback_start_try
                    invoke-static {p0}, LX/WKI;->froggoCaptureDownloadButton(Ljava/lang/Object;)V
                    sget-object v0, LX/WKI;->froggoDownloadButton:Landroid/view/View;
                    if-eqz v0, :froggo_feedback_start_no_view
                    sget-object v1, LX/WKI;->froggoDownloadSpinner:Landroid/widget/ProgressBar;
                    if-eqz v1, :froggo_feedback_start_spinner_cleared
                    instance-of v2, v0, Landroid/view/ViewGroup;
                    if-eqz v2, :froggo_feedback_start_spinner_cleared
                    check-cast v0, Landroid/view/ViewGroup;
                    invoke-virtual {v0}, Landroid/view/ViewGroup;->getOverlay()Landroid/view/ViewGroupOverlay;
                    move-result-object v2
                    invoke-virtual {v2, v1}, Landroid/view/ViewGroupOverlay;->remove(Landroid/view/View;)V
                    :froggo_feedback_start_spinner_cleared
                    const/4 v1, 0x0
                    sput-object v1, LX/WKI;->froggoDownloadSpinner:Landroid/widget/ProgressBar;
                    invoke-virtual {v0}, Landroid/view/View;->getBackgroundTintList()Landroid/content/res/ColorStateList;
                    move-result-object v1
                    sput-object v1, LX/WKI;->froggoDownloadButtonTint:Landroid/content/res/ColorStateList;
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1}, Landroid/view/View;->setEnabled(Z)V
                    const v1, 0x3e99999a
                    invoke-virtual {v0, v1}, Landroid/view/View;->setAlpha(F)V
                    instance-of v1, v0, Landroid/view/ViewGroup;
                    if-eqz v1, :froggo_feedback_start_no_spinner_host
                    check-cast v0, Landroid/view/ViewGroup;
                    new-instance v1, Landroid/widget/ProgressBar;
                    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;
                    move-result-object v2
                    invoke-direct {v1, v2}, Landroid/widget/ProgressBar;-><init>(Landroid/content/Context;)V
                    const/4 v2, 0x1
                    invoke-virtual {v1, v2}, Landroid/widget/ProgressBar;->setIndeterminate(Z)V
                    const v2, 0x3f19999a
                    invoke-virtual {v1, v2}, Landroid/view/View;->setScaleX(F)V
                    invoke-virtual {v1, v2}, Landroid/view/View;->setScaleY(F)V
                    invoke-virtual {v0}, Landroid/view/ViewGroup;->getOverlay()Landroid/view/ViewGroupOverlay;
                    move-result-object v2
                    invoke-virtual {v2, v1}, Landroid/view/ViewGroupOverlay;->add(Landroid/view/View;)V
                    sput-object v1, LX/WKI;->froggoDownloadSpinner:Landroid/widget/ProgressBar;
                    :froggo_feedback_start_no_spinner_host
                    const-string v1, "FroggoPatches"
                    const-string v2, "feedback-spinner-start"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    goto :froggo_feedback_start_done

                    :froggo_feedback_start_no_view
                    const-string v1, "FroggoPatches"
                    const-string v2, "feedback-start-no-view"
                    invoke-static {v1, v2}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

                    :froggo_feedback_start_done
                    return-void
                    .catch Ljava/lang/Throwable; {:froggo_feedback_start_try .. :froggo_feedback_start_done} :froggo_feedback_start_catch
                    :froggo_feedback_start_catch
                    move-exception v0
                    const-string v1, "FroggoPatches"
                    const-string v2, "feedback-start-exception"
                    invoke-static {v1, v2, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(animationStartHelper)

        val downloadResultHelper = ImmutableMethod(
            callbackClass.type,
            "froggoShowDownloadFeedbackResult",
            listOf(ImmutableMethodParameter("Z", null, null)),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(8),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    new-instance v0, Landroid/os/Handler;
                    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                    move-result-object v1
                    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                    new-instance v1, LX/WKI;
                    const/16 v2, 0x84
                    invoke-static {p0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                    move-result-object v3
                    const/4 v4, 0x0
                    const/4 v5, 0x0
                    invoke-direct {v1, v2, v3, v4, v5}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(downloadResultHelper)

        val storyChoiceHelper = ImmutableMethod(
            callbackClass.type,
            "froggoChooseStoryDownload",
            listOf(
                ImmutableMethodParameter("Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;", null, null),
                ImmutableMethodParameter("Landroid/view/View;", null, null),
            ),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(12),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget-object v0, p0, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A02:Lcom/facebook/stories/model/StoryCard;
                    invoke-virtual {v0}, Lcom/facebook/stories/model/StoryCard;->A0l()LX/8OX;
                    move-result-object v0
                    sget-object v1, LX/8OX;->A0D:LX/8OX;
                    if-eq v0, v1, :froggo_story_choice_video
                    invoke-static {p1}, LX/WKI;->froggoShowDownloadFeedbackStart(Ljava/lang/Object;)V
                    new-instance v0, LX/WKI;
                    const/16 v1, 0x7f
                    invoke-direct {v0, v1, p0, p0, p0}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    new-instance v1, Ljava/lang/Thread;
                    invoke-direct {v1, v0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v1}, Ljava/lang/Thread;->start()V
                    return-void

                    :froggo_story_choice_video
                    iget-object v0, p0, Lcom/facebook/stories/viewer/ui/buckets/regular/topbar/menu/StoryViewerMoreButtonCallback;->A09:Landroid/content/Context;
                    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                    move-result-object v1
                    invoke-virtual {v1}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
                    move-result-object v1
                    invoke-virtual {v1}, Landroid/content/res/Configuration;->getLocales()Landroid/os/LocaleList;
                    move-result-object v1
                    const/4 v2, 0x0
                    invoke-virtual {v1, v2}, Landroid/os/LocaleList;->get(I)Ljava/util/Locale;
                    move-result-object v1
                    invoke-virtual {v1}, Ljava/util/Locale;->getLanguage()Ljava/lang/String;
                    move-result-object v1
                    const-string v3, "es"
                    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v1
                    if-eqz v1, :froggo_story_choice_english
                    const-string v3, "Descargar historia"
                    const-string v4, "Video completo"
                    const-string v5, "Primer frame"
                    goto :froggo_story_choice_text_ready
                    :froggo_story_choice_english
                    const-string v3, "Download story"
                    const-string v4, "Full video"
                    const-string v5, "First frame"
                    :froggo_story_choice_text_ready
                    const/4 v1, 0x2
                    new-array v1, v1, [Ljava/lang/CharSequence;
                    const/4 v2, 0x0
                    aput-object v4, v1, v2
                    const/4 v2, 0x1
                    aput-object v5, v1, v2
                    new-instance v2, LX/WKI;
                    const/16 v4, 0x87
                    invoke-direct {v2, v4, p0, p1, p0}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    new-instance v4, Landroid/app/AlertDialog${'$'}Builder;
                    invoke-direct {v4, v0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
                    invoke-virtual {v4, v3}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
                    invoke-virtual {v4, v1, v2}, Landroid/app/AlertDialog${'$'}Builder;->setItems([Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
                    invoke-virtual {v4}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(storyChoiceHelper)

        val fullscreenStoryChoiceHelper = ImmutableMethod(
            callbackClass.type,
            "froggoChooseFullscreenStoryDownload",
            listOf(
                ImmutableMethodParameter(callbackClass.type, null, null),
                ImmutableMethodParameter("Ljava/lang/Object;", null, null),
            ),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(12),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget-object v0, p0, LX/WKI;->A01:Ljava/lang/Object;
                    check-cast v0, Lcom/facebook/stories/model/StoryCard;
                    invoke-virtual {v0}, Lcom/facebook/stories/model/StoryCard;->A0l()LX/8OX;
                    move-result-object v0
                    sget-object v1, LX/8OX;->A0D:LX/8OX;
                    if-eq v0, v1, :froggo_fullscreen_story_choice_video
                    invoke-static {p1}, LX/WKI;->froggoShowDownloadFeedbackStart(Ljava/lang/Object;)V
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    return-void
                    :froggo_fullscreen_story_choice_video
                    invoke-static {p1}, LX/WKI;->froggoCaptureDownloadButton(Ljava/lang/Object;)V
                    sget-object v6, LX/WKI;->froggoDownloadButton:Landroid/view/View;
                    if-eqz v6, :froggo_fullscreen_story_choice_no_view
                    invoke-virtual {v6}, Landroid/view/View;->getContext()Landroid/content/Context;
                    move-result-object v0
                    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                    move-result-object v1
                    invoke-virtual {v1}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
                    move-result-object v1
                    invoke-virtual {v1}, Landroid/content/res/Configuration;->getLocales()Landroid/os/LocaleList;
                    move-result-object v1
                    const/4 v2, 0x0
                    invoke-virtual {v1, v2}, Landroid/os/LocaleList;->get(I)Ljava/util/Locale;
                    move-result-object v1
                    invoke-virtual {v1}, Ljava/util/Locale;->getLanguage()Ljava/lang/String;
                    move-result-object v1
                    const-string v3, "es"
                    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v1
                    if-eqz v1, :froggo_fullscreen_story_choice_english
                    const-string v3, "Descargar historia"
                    const-string v4, "Video completo"
                    const-string v5, "Primer frame"
                    goto :froggo_fullscreen_story_choice_text_ready
                    :froggo_fullscreen_story_choice_english
                    const-string v3, "Download story"
                    const-string v4, "Full video"
                    const-string v5, "First frame"
                    :froggo_fullscreen_story_choice_text_ready
                    const/4 v1, 0x2
                    new-array v1, v1, [Ljava/lang/CharSequence;
                    const/4 v2, 0x0
                    aput-object v4, v1, v2
                    const/4 v2, 0x1
                    aput-object v5, v1, v2
                    new-instance v2, LX/WKI;
                    const/16 v4, 0x8a
                    invoke-direct {v2, v4, p0, p1, p0}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    new-instance v4, Landroid/app/AlertDialog${'$'}Builder;
                    invoke-direct {v4, v0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
                    invoke-virtual {v4, v3}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
                    invoke-virtual {v4, v1, v2}, Landroid/app/AlertDialog${'$'}Builder;->setItems([Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
                    invoke-virtual {v4}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
                    return-void
                    :froggo_fullscreen_story_choice_no_view
                    invoke-static {p1}, LX/WKI;->froggoShowDownloadFeedbackStart(Ljava/lang/Object;)V
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(fullscreenStoryChoiceHelper)

        val storyChoiceClickMethod = ImmutableMethod(
            callbackClass.type,
            "onClick",
            listOf(
                ImmutableMethodParameter("Landroid/content/DialogInterface;", null, null),
                ImmutableMethodParameter("I", null, null),
            ),
            "V",
            AccessFlags.PUBLIC.value,
            null,
            null,
            MutableMethodImplementation(8),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget v0, p0, LX/WKI;->${'$'}t:I
                    const/16 v1, 0x8a
                    if-ne v0, v1, :froggo_story_choice_click_header
                    iget-object v2, p0, LX/WKI;->A01:Ljava/lang/Object;
                    invoke-static {v2}, LX/WKI;->froggoShowDownloadFeedbackStart(Ljava/lang/Object;)V
                    iget-object v2, p0, LX/WKI;->A00:Ljava/lang/Object;
                    check-cast v2, LX/WKI;
                    const/4 v1, 0x0
                    if-ne p2, v1, :froggo_fullscreen_story_choice_first_frame
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, v2}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    return-void
                    :froggo_fullscreen_story_choice_first_frame
                    iget-object v3, v2, LX/WKI;->A00:Ljava/lang/Object;
                    iget-object v4, v2, LX/WKI;->A01:Ljava/lang/Object;
                    new-instance v2, LX/WKI;
                    const/16 v1, 0x89
                    const/4 v5, 0x0
                    invoke-direct {v2, v1, v3, v4, v5}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, v2}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    return-void
                    :froggo_story_choice_click_header
                    const/16 v1, 0x87
                    if-ne v0, v1, :froggo_story_choice_click_done
                    iget-object v0, p0, LX/WKI;->A01:Ljava/lang/Object;
                    invoke-static {v0}, LX/WKI;->froggoShowDownloadFeedbackStart(Ljava/lang/Object;)V
                    iget-object v0, p0, LX/WKI;->A00:Ljava/lang/Object;
                    const/4 v1, 0x0
                    if-ne p2, v1, :froggo_story_choice_first_frame
                    const/16 v1, 0x7f
                    goto :froggo_story_choice_worker_ready
                    :froggo_story_choice_first_frame
                    const/16 v1, 0x88
                    :froggo_story_choice_worker_ready
                    new-instance v2, LX/WKI;
                    invoke-direct {v2, v1, v0, v0, v0}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, v2}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    :froggo_story_choice_click_done
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(storyChoiceClickMethod)

        val storyWorkerMethod = ImmutableMethod(
            callbackClass.type,
            "froggoRunStoryDownload",
            listOf(ImmutableMethodParameter(callbackClass.type, null, null)),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(0, compactStoryDownloadWorkerInstructions(imagePathPrefix, videoPathPrefix))
        }
        callbackClass.methods.add(storyWorkerMethod)

        val storyFirstFrameWorkerMethod = ImmutableMethod(
            callbackClass.type,
            "froggoRunStoryFirstFrameDownload",
            listOf(ImmutableMethodParameter(callbackClass.type, null, null)),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(0, storyFirstFrameWorkerInstructions(imagePathPrefix))
        }
        callbackClass.methods.add(storyFirstFrameWorkerMethod)

        val fullscreenStoryWorkerMethod = ImmutableMethod(
            callbackClass.type,
            "froggoRunFullscreenStoryDownload",
            listOf(ImmutableMethodParameter(callbackClass.type, null, null)),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(0, fullscreenStoryDownloadWorkerInstructions(imagePathPrefix, videoPathPrefix))
        }
        callbackClass.methods.add(fullscreenStoryWorkerMethod)

        val reelWorkerMethod = ImmutableMethod(
            callbackClass.type,
            "froggoRunReelDownload",
            listOf(ImmutableMethodParameter(callbackClass.type, null, null)),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(16),
        ).toMutable().apply {
            addInstructions(0, compactReelDownloadWorkerInstructions(videoFolderOption.value!!))
        }
        callbackClass.methods.add(reelWorkerMethod)

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
                    :froggo_download_dispatch_try_start
                    iget v0, p0, LX/WKI;->${'$'}t:I
                    const/16 v1, 0x84
                    if-eq v0, v1, :froggo_download_result_worker
                    const/16 v1, 0x86
                    if-eq v0, v1, :froggo_download_button_cleanup
                    const/16 v1, 0x82
                    if-eq v0, v1, :froggo_toast_worker
                    const/16 v1, 0x83
                    if-eq v0, v1, :froggo_fullscreen_story_worker
                    const/16 v1, 0x7f
                    if-eq v0, v1, :froggo_story_download_worker
                    const/16 v1, 0x88
                    if-eq v0, v1, :froggo_story_first_frame_worker
                    const/16 v1, 0x89
                    if-eq v0, v1, :froggo_story_first_frame_worker
                    invoke-static {p0}, LX/WKI;->froggoRunReelDownload(LX/WKI;)V
                    goto :froggo_download_dispatch_end
                    :froggo_fullscreen_story_worker
                    invoke-static {p0}, LX/WKI;->froggoRunFullscreenStoryDownload(LX/WKI;)V
                    goto :froggo_download_dispatch_end
                    :froggo_story_download_worker
                    invoke-static {p0}, LX/WKI;->froggoRunStoryDownload(LX/WKI;)V
                    goto :froggo_download_dispatch_end
                    :froggo_story_first_frame_worker
                    invoke-static {p0}, LX/WKI;->froggoRunStoryFirstFrameDownload(LX/WKI;)V
                    goto :froggo_download_dispatch_end
                    :froggo_download_result_worker
                    sget-object v0, LX/WKI;->froggoDownloadButton:Landroid/view/View;
                    if-eqz v0, :froggo_download_dispatch_end
                    sget-object v1, LX/WKI;->froggoDownloadSpinner:Landroid/widget/ProgressBar;
                    if-eqz v1, :froggo_download_result_spinner_cleared
                    instance-of v2, v0, Landroid/view/ViewGroup;
                    if-eqz v2, :froggo_download_result_spinner_cleared
                    check-cast v0, Landroid/view/ViewGroup;
                    invoke-virtual {v0}, Landroid/view/ViewGroup;->getOverlay()Landroid/view/ViewGroupOverlay;
                    move-result-object v2
                    invoke-virtual {v2, v1}, Landroid/view/ViewGroupOverlay;->remove(Landroid/view/View;)V
                    :froggo_download_result_spinner_cleared
                    const/4 v1, 0x0
                    sput-object v1, LX/WKI;->froggoDownloadSpinner:Landroid/widget/ProgressBar;
                    invoke-virtual {v0}, Landroid/view/View;->clearAnimation()V
                    const/high16 v1, 0x3f800000
                    invoke-virtual {v0, v1}, Landroid/view/View;->setAlpha(F)V
                    const/4 v1, 0x1
                    invoke-virtual {v0, v1}, Landroid/view/View;->setEnabled(Z)V

                    iget-object v1, p0, LX/WKI;->A00:Ljava/lang/Object;
                    check-cast v1, Ljava/lang/Boolean;
                    invoke-virtual {v1}, Ljava/lang/Boolean;->booleanValue()Z
                    move-result v1
                    if-eqz v1, :froggo_download_result_failure
                    const/16 v2, 0x10
                    invoke-virtual {v0, v2}, Landroid/view/View;->performHapticFeedback(I)Z
                    const v2, 0xff4caf50
                    const-string v3, "feedback-button-success"
                    goto :froggo_download_result_style_ready
                    :froggo_download_result_failure
                    const v2, 0xfff44336
                    const-string v3, "feedback-button-failure"
                    :froggo_download_result_style_ready
                    invoke-static {v2}, Landroid/content/res/ColorStateList;->valueOf(I)Landroid/content/res/ColorStateList;
                    move-result-object v2
                    invoke-virtual {v0, v2}, Landroid/view/View;->setBackgroundTintList(Landroid/content/res/ColorStateList;)V
                    const-string v2, "FroggoPatches"
                    invoke-static {v2, v3}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                    new-instance v2, Landroid/view/animation/AlphaAnimation;
                    const v3, 0x3e99999a
                    const/high16 v4, 0x3f800000
                    invoke-direct {v2, v3, v4}, Landroid/view/animation/AlphaAnimation;-><init>(FF)V
                    const-wide/16 v3, 0xdc
                    invoke-virtual {v2, v3, v4}, Landroid/view/animation/Animation;->setDuration(J)V
                    invoke-virtual {v0, v2}, Landroid/view/View;->startAnimation(Landroid/view/animation/Animation;)V

                    new-instance v2, Landroid/os/Handler;
                    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                    move-result-object v3
                    invoke-direct {v2, v3}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                    new-instance v3, LX/WKI;
                    const/16 v4, 0x86
                    move-object v5, v0
                    const/4 v6, 0x0
                    const/4 v7, 0x0
                    invoke-direct {v3, v4, v5, v6, v7}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    const-wide/16 v4, 0x384
                    invoke-virtual {v2, v3, v4, v5}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
                    goto :froggo_download_dispatch_end

                    :froggo_download_button_cleanup
                    iget-object v0, p0, LX/WKI;->A00:Ljava/lang/Object;
                    instance-of v1, v0, Landroid/view/View;
                    if-eqz v1, :froggo_download_dispatch_end
                    check-cast v0, Landroid/view/View;
                    sget-object v1, LX/WKI;->froggoDownloadSpinner:Landroid/widget/ProgressBar;
                    if-eqz v1, :froggo_download_cleanup_spinner_cleared
                    instance-of v2, v0, Landroid/view/ViewGroup;
                    if-eqz v2, :froggo_download_cleanup_spinner_cleared
                    check-cast v0, Landroid/view/ViewGroup;
                    invoke-virtual {v0}, Landroid/view/ViewGroup;->getOverlay()Landroid/view/ViewGroupOverlay;
                    move-result-object v2
                    invoke-virtual {v2, v1}, Landroid/view/ViewGroupOverlay;->remove(Landroid/view/View;)V
                    :froggo_download_cleanup_spinner_cleared
                    const/4 v1, 0x0
                    sput-object v1, LX/WKI;->froggoDownloadSpinner:Landroid/widget/ProgressBar;
                    invoke-virtual {v0}, Landroid/view/View;->clearAnimation()V
                    const/high16 v1, 0x3f800000
                    invoke-virtual {v0, v1}, Landroid/view/View;->setAlpha(F)V
                    const/4 v1, 0x1
                    invoke-virtual {v0, v1}, Landroid/view/View;->setEnabled(Z)V
                    sget-object v1, LX/WKI;->froggoDownloadButton:Landroid/view/View;
                    if-ne v1, v0, :froggo_download_dispatch_end
                    sget-object v1, LX/WKI;->froggoDownloadButtonTint:Landroid/content/res/ColorStateList;
                    invoke-virtual {v0, v1}, Landroid/view/View;->setBackgroundTintList(Landroid/content/res/ColorStateList;)V
                    const/4 v1, 0x0
                    sput-object v1, LX/WKI;->froggoDownloadButton:Landroid/view/View;
                    sput-object v1, LX/WKI;->froggoDownloadButtonTint:Landroid/content/res/ColorStateList;
                    const-string v1, "FroggoPatches"
                    const-string v2, "feedback-button-restored"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    goto :froggo_download_dispatch_end
                    :froggo_toast_worker
                    iget-object v0, p0, LX/WKI;->A00:Ljava/lang/Object;
                    check-cast v0, Landroid/content/Context;
                    iget-object v1, p0, LX/WKI;->A01:Ljava/lang/Object;
                    check-cast v1, Ljava/lang/CharSequence;
                    invoke-static {}, Landroid/os/Looper;->myLooper()Landroid/os/Looper;
                    move-result-object v2
                    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                    move-result-object v3
                    if-ne v2, v3, :froggo_toast_repost
                    const/4 v2, 0x1
                    invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
                    move-result-object v0
                    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
                    goto :froggo_download_dispatch_end
                    :froggo_toast_repost
                    new-instance v2, Landroid/os/Handler;
                    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                    move-result-object v3
                    invoke-direct {v2, v3}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                    invoke-virtual {v2, p0}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z
                    :froggo_download_dispatch_end
                    return-void
                    .catch Ljava/lang/Throwable; {:froggo_download_dispatch_try_start .. :froggo_download_dispatch_end} :froggo_download_dispatch_catch
                    :froggo_download_dispatch_catch
                    move-exception v0
                    const-string v1, "FroggoPatches"
                    const-string v2, "dispatch exception"
                    invoke-static {v1, v2, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
                    return-void
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
            MutableMethodImplementation(8),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    iget v0, p0, LX/WKI;->${'$'}t:I
                    const/16 v1, 0x80
                    if-eq v0, v1, :froggo_download_invoke_capture_button
                    const/16 v1, 0x81
                    if-ne v0, v1, :froggo_download_invoke_check_fullscreen
                    const-string v1, "FroggoPatches"
                    const-string v2, "invoke-start"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    invoke-static {p1}, LX/WKI;->froggoShowDownloadFeedbackStart(Ljava/lang/Object;)V
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    goto :froggo_download_invoke_done
                    :froggo_download_invoke_check_fullscreen
                    const/16 v1, 0x83
                    if-ne v0, v1, :froggo_download_invoke_check_story
                    invoke-static {p0, p1}, LX/WKI;->froggoChooseFullscreenStoryDownload(LX/WKI;Ljava/lang/Object;)V
                    goto :froggo_download_invoke_done
                    :froggo_download_invoke_check_story
                    const/16 v1, 0x7f
                    if-ne v0, v1, :froggo_download_invoke_done
                    invoke-static {p1}, LX/WKI;->froggoShowDownloadFeedbackStart(Ljava/lang/Object;)V
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    goto :froggo_download_invoke_done
                    :froggo_download_invoke_capture_button
                    invoke-static {p1}, LX/WKI;->froggoCaptureDownloadButton(Ljava/lang/Object;)V
                    :froggo_download_invoke_done
                    sget-object v0, LX/0FI;->A00:LX/0FI;
                    return-object v0
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(callbackInvokeMethod)

        val videoCallbackClass = videoSaveCallback.classDef
        videoCallbackClass.interfaces.removeAll { it == "Ljava/lang/Runnable;" }
        videoCallbackClass.interfaces.add("Ljava/lang/Runnable;")
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
                move-object/from16 v1, p1
                invoke-static {v1}, LX/WKI;->froggoShowDownloadFeedbackStart(Ljava/lang/Object;)V
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

                    new-instance v1, LX/2vk;
                    const-string v2, "Download"
                    move-object v3, v0
                    invoke-direct {v1, v2, v3}, LX/2vk;-><init>(Ljava/lang/String;Lkotlin/jvm/functions/Function1;)V

                    new-instance v2, LX/WKI;
                    const/16 v3, 0x81
                    move-object/from16 v4, p1
                    move-object/from16 v5, p2
                    move-object/from16 v6, p3
                    invoke-direct {v2, v3, v4, v5, v6}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V

                    new-instance v3, LX/2QZ;
                    const-string v4, "Download"
                    move-object v5, v2
                    invoke-direct {v3, v4, v5}, LX/2QZ;-><init>(Ljava/lang/String;Lkotlin/jvm/functions/Function1;)V

                    new-instance v4, LX/9yX;
                    sget-object v5, LX/1Vq;->A80:LX/1Vq;
                    invoke-direct {v4, v5}, LX/9yX;-><init>(LX/1Vq;)V

                    move-object/from16 v6, p0
                    sget-object v7, LX/1c6;->A02:LX/1c6;
                    move-object v8, v1
                    move-object v9, v3
                    const/4 v10, 0x0
                    move-object v11, v4
                    sget-object v12, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    sget-object v13, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                    const-string v14, "download_button"
                    const/4 v15, 0x0
                    const-string v0, "Download"
                    move-object/from16 v16, v0
                    const/16 v17, 0x0
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
    }
}
/*
                 invoke-static {v7}, LX/CQu;->A10(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v1
                new-instance v2, LX/WKI;
                const/16 v3, 0x7f
                move-object v9, v1
                move-object v10, v1
                move-object v11, v1
                invoke-direct {v2, v3, v9, v10, v11}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                new-instance v1, LX/3S5;
                const/4 v3, 0x0
                invoke-direct {v1, v3, v2}, LX/3S5;-><init>(LX/3QZ;Lkotlin/jvm/functions/Function1;)V
                invoke-virtual {v0, v1}, LX/4hH;->A2C(LX/X6V;)V
                invoke-virtual {v0}, LX/Nqn;->A1O()V
                iget-object v0, v0, LX/4hH;->A00:LX/4hG;
                invoke-virtual {v4, v0}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z
            """.trimIndent(),
        )

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
        val toastHelper = ImmutableMethod(
            callbackClass.type,
            "froggoPostToast",
            listOf(
                ImmutableMethodParameter("Landroid/content/Context;", null, null),
                ImmutableMethodParameter("Ljava/lang/CharSequence;", null, null),
            ),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(8),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                    new-instance v0, Landroid/os/Handler;
                    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                    move-result-object v1
                    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                    new-instance v1, LX/WKI;
                    const/16 v2, 0x82
                    move-object v3, p0
                    move-object v4, p1
                    const/4 v5, 0x0
                    invoke-direct {v1, v2, v3, v4, v5}, LX/WKI;-><init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z
                    return-void
                """.trimIndent(),
            )
        }
        callbackClass.methods.add(toastHelper)

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
                    const/16 v1, 0x82
                    if-eq v0, v1, :froggo_toast_worker
                    const/16 v1, 0x7f
                    if-eq v0, v1, :froggo_story_download_worker
                    $compactReelDownloadWorkerInstructions
                    :froggo_story_download_worker
                    $compactStoryDownloadWorkerInstructions
                    :froggo_toast_worker
                    iget-object v0, p0, LX/WKI;->A00:Ljava/lang/Object;
                    check-cast v0, Landroid/content/Context;
                    iget-object v1, p0, LX/WKI;->A01:Ljava/lang/Object;
                    check-cast v1, Ljava/lang/CharSequence;
                    const/4 v2, 0x1
                    invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
                    move-result-object v0
                    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
                    return-void
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
                    if-ne v0, v1, :froggo_story_download_invoke_check
                    const-string v1, "FroggoPatches"
                    const-string v2, "invoke-start"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    goto :froggo_download_invoke_done
                    :froggo_story_download_invoke_check
                    const/16 v1, 0x7f
                    if-ne v0, v1, :froggo_download_invoke_done
                    new-instance v0, Ljava/lang/Thread;
                    invoke-direct {v0, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
                    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                    :froggo_download_invoke_done
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
                ImmutableMethodParameter("LX/4ia;", null, null),
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

    }
}
*/
