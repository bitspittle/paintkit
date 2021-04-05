package bitspittle.paintkit.l18n

import org.jetbrains.annotations.PropertyKey
import java.util.*

private val VAR_REGEX = """\{(\d+)}""".toRegex()

/**
 * Fetch localized text associated with the current key.
 */
@Suppress("FunctionName") // Standard way to indicate fetching localized text
fun _t(@PropertyKey(resourceBundle = "strings") key: String, vararg args: String): String {
    val bundle = ResourceBundle.getBundle("strings")

    var result = bundle.getString(key)
    VAR_REGEX.findAll(result)
        .map { match -> match.groupValues[1].toInt() }
        .toSet()
        .forEach { index -> result = result.replace("{$index}", args[index]) }

    return result
}