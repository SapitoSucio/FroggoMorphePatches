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
    val primaryName = "@android:color/system_accent1_100"
    val secondaryText = "@android:color/system_accent1_300"
    val tertiaryIcon = "@android:color/system_neutral2_600"
    val tertiaryText = "@android:color/system_accent1_300"
    val accent = "@android:color/system_accent1_200"
    val metadata = "@android:color/system_accent1_300"

    replaceStyleItem(styleName, "attr_0x7f040646", background)
    replaceStyleItem(styleName, "attr_0x7f04061c", background)
    replaceStyleItem(styleName, "attr_0x7f040628", background)
    replaceStyleItem(styleName, "attr_0x7f0405de", primaryIcon)
    replaceStyleItem(styleName, "attr_0x7f0405e1", primaryName)
    replaceStyleItem(styleName, "attr_0x7f04057a", primaryName)
    replaceStyleItem(styleName, "attr_0x7f0405cb", metadata)
    replaceStyleItem(styleName, "attr_0x7f0405a7", metadata)
    replaceStyleItem(styleName, "attr_0x7f040601", secondaryText)
    replaceStyleItem(styleName, "attr_0x7f040605", accent)
    replaceStyleItem(styleName, "attr_0x7f04060a", secondaryText)
    replaceStyleItem(styleName, "attr_0x7f04060d", metadata)
    replaceStyleItem(styleName, "attr_0x7f04062b", tertiaryIcon)
    replaceStyleItem(styleName, "attr_0x7f04062c", tertiaryText)
    replaceStyleItem(styleName, "attr_0x7f04050f", accent)
    replaceStyleItem(styleName, "attr_0x7f0404ff", accent)
    replaceStyleItem(styleName, "attr_0x7f040626", accent)
    replaceStyleItem(styleName, "attr_0x7f040627", accent)
    replaceStyleItem(styleName, "attr_0x7f040629", secondaryText)
    replaceStyleItem(styleName, "attr_0x7f04062a", secondaryText)

    // Semantic accent/border/story roles that are consumed directly by FDS.
    replaceStyleItem(styleName, "attr_0x7f040500", secondaryText) // accent deemphasized
    replaceStyleItem(styleName, "attr_0x7f040501", accent) // active dot
    replaceStyleItem(styleName, "attr_0x7f04050a", "@android:color/system_neutral1_800") // background deemphasized
    replaceStyleItem(styleName, "attr_0x7f040510", secondaryText) // focus border
    replaceStyleItem(styleName, "attr_0x7f040511", "@android:color/system_neutral2_700") // persistent border
    replaceStyleItem(styleName, "attr_0x7f040512", "@android:color/system_neutral2_700") // responsive border
    replaceStyleItem(styleName, "attr_0x7f040513", "@android:color/system_neutral2_700") // emphasis border
    replaceStyleItem(styleName, "attr_0x7f04051e", "@android:color/system_neutral2_700") // card border
    replaceStyleItem(styleName, "attr_0x7f04061a", tertiaryIcon) // story seen
    replaceStyleItem(styleName, "attr_0x7f04061b", accent) // story unseen
    replaceStyleItem(styleName, "attr_0x7f040631", "@android:color/system_neutral1_800") // text input bar
    replaceStyleItem(styleName, "attr_0x7f040635", "@android:color/system_neutral2_700") // text input inner border
    replaceStyleItem(styleName, "attr_0x7f040636", "@android:color/system_neutral2_700") // text input outer border
    replaceStyleItem(styleName, "attr_0x7f04063f", "@android:color/system_neutral1_700") // UFI icon button background

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
    replaceStyleItem(styleName, "attr_0x7f04056b", tertiaryIcon) // disabled icon
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
                // Resolve the system palette at runtime so the body color follows Monet changes.
                val bodyColor = ImmutableMethod(
                    postBodyText.classDef.type,
                    "froggoBodyColor",
                    listOf(ImmutableMethodParameter("Landroid/content/Context;", null, null)),
                    "I",
                    AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                    null, null, MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(0, """
                        invoke-static {p0}, LX/1yy;->A06(Landroid/content/Context;)Z
                        move-result v0
                        if-nez v0, :froggo_dark_body
                        const/4 v0, 0x0
                        return v0
                        :froggo_dark_body
                        sget v0, Landroid/R${'$'}color;->system_neutral1_200:I
                        invoke-virtual {p0, v0}, Landroid/content/Context;->getColor(I)I
                        move-result v0
                        return v0
                    """.trimIndent())
                }
                postBodyText.classDef.methods.add(bodyColor)

                // A23(0) selects the default body color; explicit media/custom colors survive.
                val postMethod = postBodyText.method
                require(postMethod.implementation!!.registerCount == 45)
                val postAnchor = postMethod.implementation!!.instructions.withIndex().filter { (_, instruction) ->
                    (instruction as? ReferenceInstruction)?.reference.toString() == "LX/313;->A23(I)V"
                }.single().index
                val postDone = postMethod.implementation!!.newLabelForIndex(postAnchor)
                postMethod.addInstructions(postAnchor, """
                    iget-object v1, v3, LX/3QZ;->A0C:Landroid/content/Context;
                    invoke-static {v1}, LX/30L;->froggoBodyColor(Landroid/content/Context;)I
                    move-result v1
                """.trimIndent())
                postMethod.implementation!!.addInstruction(postAnchor, BuilderInstruction21t(Opcode.IF_NEZ, 1, postDone))

                // Branches bind to this method's locations, not detached snippet offsets.
                listOf(postMethod).forEach { method ->
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
                document("res/values/style.2s.xml").use { styles ->
                    // ThemePreferences applies 0x7f20022b/22c for dark.
                    styles.applyMaterialYouStyle("style.2_0x7f20022b")
                    styles.applyMaterialYouStyle("style.2_0x7f20022c")
                }

                document("res/values/colors.xml").use { colors ->
                    // FDS/MIG components frequently bypass the root theme and read these directly.
                    // Keep every override as an Android dynamic-color reference, never a preview hex.
                    colors.replaceColorValue("color_0x7f060001", "@android:color/system_accent1_500")
                    colors.replaceColorValue("color_0x7f060002", "@android:color/system_neutral1_10")
                    colors.replaceColorValue("color_0x7f060003", "@android:color/system_neutral1_900")
                    colors.replaceColorValue("color_0x7f060004", "@android:color/system_neutral1_800")
                    colors.replaceColorValue("color_0x7f060005", "@android:color/system_neutral2_700")
                    colors.replaceColorValue("color_0x7f0600a8", "@android:color/system_accent1_500")
                    colors.replaceColorValue("color_0x7f0602d4", "@android:color/system_accent1_500")
                    colors.replaceColorValue("color_0x7f06035c", "@android:color/system_accent1_500")
                    colors.replaceColorValue("color_0x7f0601fb", "@android:color/system_neutral2_900")
                    colors.replaceColorValue("color_0x7f060153", "@android:color/system_neutral1_800")
                }

                document("res/values-night/colors.xml").use { colors ->
                    colors.replaceColorValue("color_0x7f060002", "@android:color/system_neutral1_900")
                    colors.replaceColorValue("color_0x7f060003", "@android:color/system_neutral1_50")
                    colors.replaceColorValue("color_0x7f060004", "@android:color/system_neutral1_10")
                    colors.replaceColorValue("color_0x7f060005", "@android:color/system_neutral2_200")
                    colors.replaceColorValue("color_0x7f060463", "@android:color/system_neutral1_900")
                    colors.replaceColorValue("color_0x7f060464", "@android:color/system_accent1_200")
                    colors.replaceColorValue("color_0x7f060465", "@android:color/system_neutral1_800")
                    colors.replaceColorValue("color_0x7f060466", "@android:color/system_neutral1_50")
                }
            }

            else -> error("Unsupported Facebook theme option: ${themeOption.value}")
        }
    }
}
