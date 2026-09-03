package app.froggo.patches.facebook.theme

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import org.w3c.dom.Element

private const val FACEBOOK_DARK_CARD_COLOR = -13421772 // #ff333334

private val addToStorySplitCard = Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "LX/2rj;",
        "LX/2sF;",
        "Lcom/facebook/common/callercontext/CallerContext;",
        "LX/3QZ;",
        "Ljava/lang/Object;",
        "LX/UHO;",
        "F",
        "F",
        "F",
        "F",
        "I",
        "I",
    ),
    custom = { method, classDef ->
        classDef.type == "LX/UHO;" && method.name == "A00"
    },
)

private val addToStoryPlusButton = Fingerprint(
    returnType = "LX/3Pu;",
    parameters = listOf("LX/24H;"),
    custom = { method, classDef ->
        classDef.type == "LX/2t3;" && method.name == "render"
    },
)

private val darkerDarkModeColors = Fingerprint(
    returnType = "Lcom/facebook/dsp/core/ColorData;",
    parameters = listOf("LX/1y5;"),
    custom = { method, classDef ->
        classDef.type == "LX/3l4;" && method.name == "AQL"
    },
)

private val darkestDarkModeColors = Fingerprint(
    returnType = "Lcom/facebook/dsp/core/ColorData;",
    parameters = listOf("LX/1y5;"),
    custom = { method, classDef ->
        classDef.type == "LX/3l6;" && method.name == "AQL"
    },
)

private val navigationColors = listOf("A01", "A02", "A04", "A05").associateWith { name ->
    Fingerprint(
        returnType = "I",
        parameters = emptyList(),
        custom = { method, classDef -> classDef.type == "LX/25J;" && method.name == name },
    )
}

private val commentBodyText = Fingerprint(
    returnType = "LX/3Pu;",
    parameters = listOf("LX/24H;"),
    custom = { method, classDef -> classDef.type == "LX/8HT;" && method.name == "render" },
)

private val postBodyText = Fingerprint(
    returnType = "LX/3Pu;",
    parameters = listOf("LX/3QZ;"),
    custom = { method, classDef -> classDef.type == "LX/30L;" && method.name == "A1K" },
)

private fun org.w3c.dom.Document.replaceStyleItem(styleName: String, itemName: String, value: String) {
    val styles = getElementsByTagName("style.2")
    for (styleIndex in 0 until styles.length) {
        val style = styles.item(styleIndex) as? Element ?: continue
        if (style.getAttribute("name") != styleName) continue

        val items = style.getElementsByTagName("item")
        for (itemIndex in 0 until items.length) {
            val item = items.item(itemIndex) as? Element ?: continue
            if (item.getAttribute("name") == itemName) {
                item.textContent = value
                return
            }
        }

        val item = createElement("item")
        item.setAttribute("name", itemName)
        item.textContent = value
        style.appendChild(item)
        return
    }
    error("Facebook 573 style resource '$styleName' was not found")
}

private fun org.w3c.dom.Document.replaceColorValue(colorName: String, value: String) {
    val colors = getElementsByTagName("color")
    for (index in 0 until colors.length) {
        val color = colors.item(index) as? Element ?: continue
        if (color.getAttribute("name") == colorName) {
            color.textContent = value
            return
        }
    }
    error("Facebook 573 color resource '$colorName' was not found")
}

private fun org.w3c.dom.Document.applyMaterialYouStyle(styleName: String) {
    val background = "@android:color/system_neutral1_900"
    val primaryIcon = "@android:color/system_accent1_200"
    val secondaryText = "@android:color/system_accent1_300"
    val tertiaryText = "@android:color/system_neutral2_600"
    val accent = "@android:color/system_accent1_200"
    val metadata = "@color/froggo_theme_metadata"

    replaceStyleItem(styleName, "attr_0x7f040646", background)
    replaceStyleItem(styleName, "attr_0x7f04061c", background)
    replaceStyleItem(styleName, "attr_0x7f040628", background)
    replaceStyleItem(styleName, "attr_0x7f0405de", primaryIcon)
    // Approved soft hierarchy: pale names, mid-tone body copy, quieter metadata.
    replaceStyleItem(styleName, "attr_0x7f0405e1", "@color/froggo_theme_name")
    replaceStyleItem(styleName, "attr_0x7f04057a", "@color/froggo_theme_name")
    replaceStyleItem(styleName, "attr_0x7f0405cb", metadata)
    replaceStyleItem(styleName, "attr_0x7f0405a7", metadata)
    replaceStyleItem(styleName, "attr_0x7f040601", secondaryText)
    replaceStyleItem(styleName, "attr_0x7f040605", accent)
    replaceStyleItem(styleName, "attr_0x7f04060a", secondaryText)
    replaceStyleItem(styleName, "attr_0x7f04060d", metadata)
    replaceStyleItem(styleName, "attr_0x7f04062b", tertiaryText)
    replaceStyleItem(styleName, "attr_0x7f04062c", tertiaryText)
    replaceStyleItem(styleName, "attr_0x7f04050f", accent)
    replaceStyleItem(styleName, "attr_0x7f0404ff", accent)
    replaceStyleItem(styleName, "attr_0x7f040626", accent)
    replaceStyleItem(styleName, "attr_0x7f040627", accent)
    replaceStyleItem(styleName, "attr_0x7f040629", secondaryText)
    replaceStyleItem(styleName, "attr_0x7f04062a", secondaryText)

    // Darker posts; secondary containers keep their own lighter surface.
    replaceStyleItem(styleName, "attr_0x7f04061c", "@android:color/system_neutral2_900")
    replaceStyleItem(styleName, "attr_0x7f040518", "@android:color/system_neutral2_900")
    replaceStyleItem(styleName, "attr_0x7f04051a", "@android:color/system_neutral2_900")

    // Alternate trays and neutral controls remain distinct from the post surface.
    replaceStyleItem(styleName, "attr_0x7f040509", "@android:color/system_neutral1_800")
    replaceStyleItem(styleName, "attr_0x7f040519", "@android:color/system_neutral1_800")
    replaceStyleItem(styleName, "attr_0x7f0405fb", "@android:color/system_neutral1_800")
    replaceStyleItem(styleName, "attr_0x7f0405fc", "@android:color/system_neutral1_700")
    replaceStyleItem(styleName, "attr_0x7f040600", "@android:color/system_neutral1_800")

    // FDS semantic roles; media overlays retain their own contrast treatment.
    replaceStyleItem(styleName, "attr_0x7f04052b", "@android:color/system_neutral2_800") // comments
    replaceStyleItem(styleName, "attr_0x7f0405aa", "@android:color/system_neutral2_800") // navigation
    replaceStyleItem(styleName, "attr_0x7f0405ce", "@android:color/system_neutral1_800") // popovers
    replaceStyleItem(styleName, "attr_0x7f040571", "@android:color/system_neutral2_700") // divider
    replaceStyleItem(styleName, "attr_0x7f04056b", tertiaryText) // disabled icon
    replaceStyleItem(styleName, "attr_0x7f04056e", tertiaryText) // disabled text
    replaceStyleItem(styleName, "attr_0x7f0405d1", accent) // primary button background
    replaceStyleItem(styleName, "attr_0x7f0405d4", background) // icon on accent button
    replaceStyleItem(styleName, "attr_0x7f0405d8", background) // text on accent button
}

@Suppress("unused")
val changeFacebookTheme573Patch = resourcePatch(
    name = "Change Facebook app theme (573)",
    description = "Adds AMOLED Black and Material You palettes while preserving Facebook's light/dark mode selection.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    val themeOption = stringOption(
        key = "theme",
        default = "Material You",
        values = mapOf(
            "AMOLED Black" to "AMOLED Black",
            "Material You" to "Material You",
        ),
        title = "Theme",
        description = "AMOLED Black changes Facebook's dark palette. Material You uses Android dynamic colors in dark mode; light mode stays unchanged.",
        required = true,
    )

    dependsOn(bytecodePatch {
        execute {
            if (themeOption.value == "Material You") {
                // This helper is called only by post/comment body consumers, never by FDSColors.
                val bodyColor = ImmutableMethod(
                    commentBodyText.classDef.type,
                    "froggoBodyColor",
                    listOf(ImmutableMethodParameter("Landroid/content/Context;", null, null)),
                    "I",
                    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                    null, null, MutableMethodImplementation(5),
                ).toMutable().apply {
                    addInstructions(0, """
                        invoke-static {p0}, LX/1yy;->A06(Landroid/content/Context;)Z
                        move-result v0
                        if-nez v0, :froggo_dark_body
                        const/4 v0, 0x0
                        return v0
                        :froggo_dark_body
                        invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                        move-result-object v0
                        const-string v1, "froggo_theme_body"
                        const-string v2, "color"
                        invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
                        move-result-object v3
                        invoke-virtual {v0, v1, v2, v3}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
                        move-result v0
                        invoke-virtual {p0, v0}, Landroid/content/Context;->getColor(I)I
                        move-result v0
                        return v0
                    """.trimIndent())
                }
                commentBodyText.classDef.methods.add(bodyColor)

                val commentMethod = commentBodyText.method
                require(commentMethod.implementation!!.registerCount == 27)
                val commentAnchor = commentMethod.implementation!!.instructions.withIndex().filter { (_, instruction) ->
                    (instruction as? ReferenceInstruction)?.reference.toString() == "LX/Fky;->AAS(Landroid/text/Spannable;I)Z"
                }.single().index
                val commentDone = commentMethod.implementation!!.newLabelForIndex(commentAnchor + 1)
                commentMethod.addInstructions(commentAnchor + 1, """
                    invoke-static {v14}, LX/8HT;->froggoBodyColor(Landroid/content/Context;)I
                    move-result v1
                    new-instance v0, Landroid/text/style/ForegroundColorSpan;
                    invoke-direct {v0, v1}, Landroid/text/style/ForegroundColorSpan;-><init>(I)V
                    invoke-virtual {v6}, Landroid/text/SpannableStringBuilder;->length()I
                    move-result v7
                    const v8, 0xff0021
                    invoke-virtual {v6, v0, v4, v7, v8}, Landroid/text/SpannableStringBuilder;->setSpan(Ljava/lang/Object;III)V
                """.trimIndent())
                commentMethod.implementation!!.addInstruction(commentAnchor + 3, BuilderInstruction21t(Opcode.IF_EQZ, 1, commentDone))

                // A23(0) selects the default body color; explicit media/custom colors survive.
                val postMethod = postBodyText.method
                require(postMethod.implementation!!.registerCount == 45)
                val postAnchor = postMethod.implementation!!.instructions.withIndex().filter { (_, instruction) ->
                    (instruction as? ReferenceInstruction)?.reference.toString() == "LX/313;->A23(I)V"
                }.single().index
                val postDone = postMethod.implementation!!.newLabelForIndex(postAnchor)
                postMethod.addInstructions(postAnchor, """
                    iget-object v1, v3, LX/3QZ;->A0C:Landroid/content/Context;
                    invoke-static {v1}, LX/8HT;->froggoBodyColor(Landroid/content/Context;)I
                    move-result v1
                """.trimIndent())
                postMethod.implementation!!.addInstruction(postAnchor, BuilderInstruction21t(Opcode.IF_NEZ, 1, postDone))

                // Branches bind to this method's locations, not detached snippet offsets.
                listOf(commentMethod, postMethod).forEach { method ->
                    val instructions = method.implementation!!.instructions.toList()
                    val addresses = instructions.runningFold(0) { address, instruction -> address + instruction.codeUnits }.dropLast(1)
                    instructions.forEachIndexed { index, instruction ->
                        if (instruction is OffsetInstruction) {
                            require(addresses[index] + instruction.codeOffset in addresses) {
                                "Invalid branch target in ${method.definingClass}.${method.name} at ${addresses[index]}"
                            }
                        }
                    }
                }

                // Default tab-bar consumer: preserve Facebook's light-mode path.
                // A01/A02 supply surfaces; A04/A05 supply selected/unselected tints.
                navigationColors.forEach { (name, fingerprint) ->
                    val color = when (name) {
                        "A04" -> "system_accent1_200"
                        "A05" -> "system_accent1_300"
                        else -> "system_neutral2_800"
                    }
                    require(fingerprint.method.implementation!!.registerCount >= 3)
                    fingerprint.method.addInstructions(0, """
                        iget-object v0, p0, LX/25J;->A00:Landroid/content/Context;
                        invoke-static {v0}, LX/1yy;->A06(Landroid/content/Context;)Z
                        move-result v1
                        if-eqz v1, :froggo_original_navigation
                        sget v1, Landroid/R${'$'}color;->$color:I
                        invoke-virtual {v0, v1}, Landroid/content/Context;->getColor(I)I
                        move-result v0
                        return v0
                        :froggo_original_navigation
                    """.trimIndent())
                }
                val splitCardInstructions = addToStorySplitCard.method.implementation!!.instructions
                val splitCardLiterals = splitCardInstructions.withIndex().mapNotNull { (index, instruction) ->
                    if ((instruction as? NarrowLiteralInstruction)?.narrowLiteral == FACEBOOK_DARK_CARD_COLOR) {
                        index
                    } else {
                        null
                    }
                }
                require(splitCardLiterals.size == 1) {
                    "Expected exactly one #333334 dark card literal in UHO.A00"
                }
                addToStorySplitCard.method.addInstructions(
                    splitCardLiterals.single() + 1,
                    """
                        iget-object v6, v14, LX/3QZ;->A0C:Landroid/content/Context;
                        sget v1, Landroid/R${'$'}color;->system_neutral1_800:I
                        invoke-virtual {v6, v1}, Landroid/content/Context;->getColor(I)I
                        move-result v6
                    """.trimIndent(),
                )

                val plusButtonInstructions = addToStoryPlusButton.method.implementation!!.instructions
                val plusButtonLiterals = plusButtonInstructions.withIndex().mapNotNull { (index, instruction) ->
                    if ((instruction as? NarrowLiteralInstruction)?.narrowLiteral == FACEBOOK_DARK_CARD_COLOR) {
                        index
                    } else {
                        null
                    }
                }
                require(plusButtonLiterals.size == 1) {
                    "Expected exactly one #333334 dark card literal in 2t3.render"
                }
                addToStoryPlusButton.method.addInstructions(
                    plusButtonLiterals.single() + 1,
                    """
                        iget-object v8, v1, LX/3QZ;->A0C:Landroid/content/Context;
                        sget v0, Landroid/R${'$'}color;->system_neutral1_800:I
                        invoke-virtual {v8, v0}, Landroid/content/Context;->getColor(I)I
                        move-result v8
                    """.trimIndent(),
                )

                // DARKER_DARK_MODE / DARKEST_DARK_MODE override CARD_BACKGROUND and
                // CARD_BACKGROUND_FLAT with #252728. Returning null for just those
                // tokens makes the FDS resolver continue to the Material You values
                // in the root theme instead of stopping at Facebook's DSP spectrum.
                val disableHardcodedDarkCardColors = """
                    sget-object p0, LX/1y5;->A0O:LX/1y5;
                    if-eq p1, p0, :froggo_material_you_card
                    sget-object p0, LX/1y5;->A0Q:LX/1y5;
                    if-ne p1, p0, :froggo_material_you_card_done
                    :froggo_material_you_card
                    const/4 p0, 0x0
                    return-object p0
                    :froggo_material_you_card_done
                """.trimIndent()
                darkerDarkModeColors.method.addInstructions(0, disableHardcodedDarkCardColors)
                darkestDarkModeColors.method.addInstructions(0, disableHardcodedDarkCardColors)

            }
        }
    })

    execute {
        when (themeOption.value) {
            "AMOLED Black" -> {
                document("res/values/style.2s.xml").use { styles ->
                    // ThemePreferences applies one of these two FDS dark themes at runtime.
                    styles.replaceStyleItem("style.2_0x7f20022b", "attr_0x7f040646", "#ff000000")
                    styles.replaceStyleItem("style.2_0x7f20022c", "attr_0x7f040646", "#ff000000")
                }
            }

            "Material You" -> {
                // Android's native tonal conversion retains the wallpaper hue without
                // hardcoding the green preview. L* separates text by perceived lightness.
                mapOf("name" to 92, "body" to 82, "metadata" to 65).forEach { (role, tone) ->
                    get("res/color/froggo_theme_$role.xml").apply {
                        parentFile.mkdirs()
                        writeText("""
                            <selector xmlns:android="http://schemas.android.com/apk/res/android">
                                <item android:color="@android:color/system_neutral2_600" android:lStar="$tone" />
                            </selector>
                        """.trimIndent())
                    }
                }
                document("res/values/style.2s.xml").use { styles ->
                    // ThemePreferences applies 0x7f20022b/22c for dark.
                    styles.applyMaterialYouStyle("style.2_0x7f20022b")
                    styles.applyMaterialYouStyle("style.2_0x7f20022c")
                }

                document("res/values/colors.xml").use { colors ->
                    // FDS/MIG components frequently resolve these resources directly instead of
                    // consulting the root theme attributes. 0x7f0601fb is the direct dark card
                    // surface used by several feed/search components; keep it distinct from WASH.
                    colors.replaceColorValue("color_0x7f0601fb", "@android:color/system_neutral2_900")
                    colors.replaceColorValue("color_0x7f060153", "@android:color/system_neutral1_800")
                }

                document("res/values-night/colors.xml").use { colors ->
                    // This night-qualified surface is also fetched directly by some views.
                    colors.replaceColorValue("color_0x7f060463", "@android:color/system_neutral1_900")
                }
            }

            else -> error("Unsupported Facebook theme option: ${themeOption.value}")
        }
    }
}
