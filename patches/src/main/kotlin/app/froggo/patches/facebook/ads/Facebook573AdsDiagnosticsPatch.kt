/*
 * Facebook 573.0.0.37.74 / 473623755
 *
 * Opt-in diagnostics for locating ad delivery/rendering paths. This patch does
 * not change control flow or dump request data; it only writes fixed route
 * labels to Android Logcat.
 */
package app.froggo.patches.facebook.ads

import app.froggo.patches.shared.Constants.COMPATIBILITY_FACEBOOK_573
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

private fun redexRunnable(originalName: String) = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, classDef ->
        method.name == "run" && classDef.fields.any { field ->
            field.name == "__redex_internal_original_name" &&
                (field.initialValue as? StringEncodedValue)?.value == originalName
        }
    },
)

private fun exactMethod(
    classDescriptor: String,
    methodName: String,
    parameters: List<String> = emptyList(),
) = Fingerprint(
    parameters = parameters,
    custom = { method, classDef ->
        classDef.type == classDescriptor && method.name == methodName
    },
)

private val feedTailLoad = redexRunnable(
    "MainFeedCSRDataLoaderImpl\$maybeDoAsyncAdsTailLoad\$1",
)

private val storyAdsInsertion = redexRunnable(
    "AdBucketDataSourceUtil\$attemptAdsInsertion\$1",
)

private val storyAdsFetchMore = redexRunnable(
    "AdBucketDataSourceUtil\$attemptFetchMoreAds\$1",
)

private val storyAdsDeferredFetch = redexRunnable(
    "AdBucketDataSourceUtil\$fetchDeferredAds\$1",
)

private val feedAdsChannel = exactMethod(
    "LX/23I;",
    "A0F",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/3pN;",
        "Lcom/facebook/graphql/executor/GraphQLResult;",
    ),
)

private val feedAdsResponseConverter = exactMethod(
    "LX/bZU;",
    "A00",
    listOf(
        "Lcom/facebook/api/feed/model/FetchFeedParams;",
        "LX/3pN;",
        "LX/41R;",
    ),
)

private val storyAdsBucketInsertion = exactMethod(
    "LX/AuI;",
    "Doc",
    listOf("LX/9XO;"),
)

private val videoAdBreakFetch = exactMethod(
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

private val videoAdBreakSuccess = exactMethod(
    "LX/Qsw;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val reelsVideoAdFetch = exactMethod(
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

private val reelsBannerAdSuccess = exactMethod(
    "LX/62B;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val reelsVideoAdSuccess = exactMethod(
    "LX/9mO;",
    "onSuccess",
    listOf("Ljava/lang/Object;"),
)

private val asyncFeedAdsController = exactMethod(
    "LX/3JX;",
    "A0F",
    listOf(
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "LX/3pN;",
        "Lcom/facebook/graphql/executor/GraphQLResult;",
    ),
)

private val multiAdsSponsoredData = exactMethod(
    "Lcom/facebook/graphql/model/GraphQLFBMultiAdsFeedUnit;",
    "A00",
)

private val partialStorySponsoredData = exactMethod(
    "Lcom/facebook/graphql/model/GraphQLPartialStory;",
    "getSponsoredData",
)

private val diagnosticsProbe = exactMethod(
    "LX/2Q7;",
    "onResume",
)

private fun Instruction.registersUsed(): List<Int> = when (this) {
    is FiveRegisterInstruction -> when (registerCount) {
        0 -> emptyList()
        1 -> listOf(registerC)
        2 -> listOf(registerC, registerD)
        3 -> listOf(registerC, registerD, registerE)
        4 -> listOf(registerC, registerD, registerE, registerF)
        else -> listOf(registerC, registerD, registerE, registerF, registerG)
    }
    is ThreeRegisterInstruction -> listOf(registerA, registerB, registerC)
    is TwoRegisterInstruction -> listOf(registerA, registerB)
    is OneRegisterInstruction -> listOf(registerA)
    is RegisterRangeInstruction -> (startRegister until (startRegister + registerCount)).toList()
    else -> emptyList()
}

private fun Instruction.writeRegister(): Int? {
    val opcode = opcode.name
    val writesRegister = opcode.startsWith(
        prefix = "MOVE",
    ) || opcode.startsWith("CONST") || opcode.startsWith("IGET") ||
        opcode.startsWith("SGET") || opcode.startsWith("AGET") ||
        opcode.startsWith("NEW_INSTANCE") || opcode.startsWith("NEW_ARRAY") ||
        opcode.startsWith("ARRAY_LENGTH") || opcode.startsWith("INSTANCE_OF") ||
        opcode.startsWith("NEG_") || opcode.startsWith("NOT_") ||
        opcode.startsWith("ADD_") || opcode.startsWith("SUB_") ||
        opcode.startsWith("MUL_") || opcode.startsWith("DIV_") ||
        opcode.startsWith("REM_") || opcode.startsWith("AND_") ||
        opcode.startsWith("OR_") || opcode.startsWith("XOR_") ||
        opcode.startsWith("SHL_") || opcode.startsWith("SHR_") ||
        opcode.startsWith("USHR_") || opcode.startsWith("INT_TO_") ||
        opcode.startsWith("LONG_TO_") || opcode.startsWith("FLOAT_TO_") ||
        opcode.startsWith("DOUBLE_TO_") || opcode.startsWith("CMP_")

    if (!writesRegister) return null

    return when (this) {
        is OneRegisterInstruction -> registerA
        is TwoRegisterInstruction -> registerA
        is ThreeRegisterInstruction -> registerA
        else -> null
    }
}

private fun findSafeLowRegister(method: MutableMethod): Int? {
    val implementation = method.implementation ?: return null
    val usedRegisters = mutableSetOf<Int>()

    for (instruction in implementation.instructions) {
        val registers = instruction.registersUsed()
        val writeRegister = instruction.writeRegister()
        if (writeRegister != null && writeRegister < 16 && writeRegister !in usedRegisters &&
            registers.count { it == writeRegister } <= 1
        ) {
            return writeRegister
        }
        usedRegisters += registers
    }

    return null
}

private fun logProbe(method: MutableMethod) {
    // In the exact 573 APK, LX/2Q7.onResume has 28 registers and v0 is a
    // safe local. Keep this probe independent of the best-effort scanner so
    // its absence distinguishes patch selection/application from route miss.
    method.addInstructions(
        0,
        """
            const-string v0, "FroggoAds573/probe"
            invoke-static {v0, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent(),
    )
}

private fun logRoute(method: MutableMethod, label: String) {
    val register = findSafeLowRegister(method) ?: return

    // Log.w uses a 4-bit register list and is visible in viewers that hide
    // informational entries. Skip a route rather than producing an invalid
    // patch if this method has no safe low register available.
    method.addInstructions(
        0,
        """
            const-string v$register, "$label"
            invoke-static {v$register, v$register}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent(),
    )
}

@Suppress("unused")
val logFacebookAdsRoutes573Patch = bytecodePatch(
    name = "Log Facebook ad routes (573)",
    description = "Opt-in Logcat route markers for feed, Reels/video, and Story Ads diagnostics.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_FACEBOOK_573)

    execute {
        logProbe(diagnosticsProbe.method)
        logRoute(feedTailLoad.method, "FroggoAds573/ftail")
        logRoute(storyAdsInsertion.method, "FroggoAds573/sins")
        logRoute(storyAdsFetchMore.method, "FroggoAds573/sfetch")
        logRoute(storyAdsDeferredFetch.method, "FroggoAds573/sdefer")
        logRoute(feedAdsChannel.method, "FroggoAds573/fchan")
        logRoute(feedAdsResponseConverter.method, "FroggoAds573/fconv")
        logRoute(storyAdsBucketInsertion.method, "FroggoAds573/sbucket")
        logRoute(videoAdBreakFetch.method, "FroggoAds573/vfetch")
        logRoute(videoAdBreakSuccess.method, "FroggoAds573/vok")
        logRoute(reelsVideoAdFetch.method, "FroggoAds573/rvfetch")
        logRoute(reelsBannerAdSuccess.method, "FroggoAds573/rbok")
        logRoute(reelsVideoAdSuccess.method, "FroggoAds573/rvok")
        logRoute(asyncFeedAdsController.method, "FroggoAds573/async")
        logRoute(multiAdsSponsoredData.method, "FroggoAds573/gmulti")
        logRoute(partialStorySponsoredData.method, "FroggoAds573/gstory")
    }
}
