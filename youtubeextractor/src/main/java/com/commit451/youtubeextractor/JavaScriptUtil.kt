package com.commit451.youtubeextractor

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function

/**
 * Runs JavaScripty things
 */
internal object JavaScriptUtil {

    private const val DECRYPTION_FUNC_NAME = "decrypt"
    private const val DECRYPTION_SIGNATURE_FUNCTION_REGEX = "([\\w$]+)\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)\\s*;"
    private const val DECRYPTION_SIGNATURE_FUNCTION_REGEX_2 = "\\b([\\w$]{2})\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)\\s*;"
    private const val DECRYPTION_AKAMAIZED_STRING_REGEX = "yt\\.akamaized\\.net/\\)\\s*\\|\\|\\s*.*?\\s*c\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\("
    private const val DECRYPTION_AKAMAIZED_SHORT_STRING_REGEX = "\\bc\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\("

    private val decryptionFunctions = listOf(
        DECRYPTION_SIGNATURE_FUNCTION_REGEX_2,
        DECRYPTION_SIGNATURE_FUNCTION_REGEX,
        DECRYPTION_AKAMAIZED_SHORT_STRING_REGEX,
        DECRYPTION_AKAMAIZED_STRING_REGEX
    )

    fun loadDecryptionCode(playerCode: String): String {
        val decryptionFunctionName = decryptionFunctionName(playerCode)
        val functionPattern = "(" + decryptionFunctionName.replace("$", "\\$") + "=function\\([a-zA-Z0-9_]+\\)\\{.+?\\})"
        val decryptionFunction = "var " + Util.matchGroup1(functionPattern, playerCode) + ";"

        val helperObjectName = Util.matchGroup1(";([A-Za-z0-9_\\$]{2})\\...\\(", decryptionFunction)
        val helperPattern = "(var " + helperObjectName.replace("$", "\\$") + "=\\{.+?\\}\\};)"
        val helperObject = Util.matchGroup1(helperPattern, playerCode.replace("\n", ""))
        val callerFunction = "function $DECRYPTION_FUNC_NAME(a){return $decryptionFunctionName(a);}"

        return helperObject + decryptionFunction + callerFunction
    }

    private fun decryptionFunctionName(playerCode: String): String {
        decryptionFunctions.forEach {
            try {
                return Util.matchGroup1(it, playerCode)
            } catch (e: Exception) { }
        }
        throw IllegalStateException("Could not find decryption function with any of the patterns. Please open a new issue")
    }

    fun decryptSignature(encryptedSig: String, decryptionCode: String): String {
        val context = Context.enter()
        context.optimizationLevel = -1
        val result: Any?
        try {
            val scope = context.initStandardObjects()
            context.evaluateString(scope, decryptionCode, "decryptionCode", 1, null)
            val decryptionFunc = scope.get("decrypt", scope) as Function
            result = decryptionFunc.call(context, scope, scope, arrayOf<Any>(encryptedSig))
        } catch (e: Exception) {
            throw e
        } finally {
            Context.exit()
        }
        return result?.toString() ?: ""
    }
}
