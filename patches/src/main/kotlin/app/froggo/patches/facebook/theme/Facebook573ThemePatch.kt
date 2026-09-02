package app.froggo.patches.facebook.theme

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import org.w3c.dom.Element

private fun org.w3c.dom.Document.replaceColor(name: String, value: String) {
    val colors = getElementsByTagName("color")
    for (index in 0 until colors.length) {
        val color = colors.item(index) as? Element ?: continue
        if (color.getAttribute("name") == name) {
            color.textContent = value
            return
        }
    }
    error("Facebook 573 color resource '$name' was not found")
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
        description = "AMOLED Black changes only Facebook's dark palette. Material You follows the system dynamic palette in both light and dark mode.",
        required = true,
    )

    execute {
        when (themeOption.value) {
            "AMOLED Black" -> {
                document("res/values-night/colors.xml").use { colors ->
                    // These are Facebook 573's primary and secondary dark surfaces.
                    // Light resources are deliberately untouched.
                    colors.replaceColor("color_0x7f060002", "#ff000000")
                    colors.replaceColor("color_0x7f060463", "#ff000000")
                    colors.replaceColor("color_0x7f060465", "#ff121212")
                }
            }

            "Material You" -> {
                document("res/values/colors.xml").use { colors ->
                    colors.replaceColor("color_0x7f060001", "@android:color/system_accent1_500")
                    colors.replaceColor("color_0x7f060002", "@android:color/system_neutral1_10")
                    colors.replaceColor("color_0x7f060003", "@android:color/system_neutral1_900")
                    colors.replaceColor("color_0x7f060004", "@android:color/system_neutral1_800")
                    colors.replaceColor("color_0x7f060005", "@android:color/system_neutral2_700")
                    colors.replaceColor("color_0x7f0600a8", "@android:color/system_accent1_500")
                    colors.replaceColor("color_0x7f0602d4", "@android:color/system_accent1_500")
                    colors.replaceColor("color_0x7f06035c", "@android:color/system_accent1_500")
                }

                document("res/values-night/colors.xml").use { colors ->
                    colors.replaceColor("color_0x7f060002", "@android:color/system_neutral1_900")
                    colors.replaceColor("color_0x7f060003", "@android:color/system_neutral1_50")
                    colors.replaceColor("color_0x7f060004", "@android:color/system_neutral1_10")
                    colors.replaceColor("color_0x7f060005", "@android:color/system_neutral2_200")
                    colors.replaceColor("color_0x7f060463", "@android:color/system_neutral1_900")
                    colors.replaceColor("color_0x7f060464", "@android:color/system_accent1_200")
                    colors.replaceColor("color_0x7f060465", "@android:color/system_neutral1_800")
                    colors.replaceColor("color_0x7f060466", "@android:color/system_neutral1_50")
                }
            }

            else -> error("Unsupported Facebook theme option: ${themeOption.value}")
        }
    }
}
