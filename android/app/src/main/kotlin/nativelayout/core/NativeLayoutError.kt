/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

open class NativeLayoutError(
    override val message: String,
    val line: Int,
    val context: String,
    val word: String? = null,
    val suggestion: String? = null
) : Exception(message) {
    fun format(): String = buildString {
        append("Error: line $line\n$context\n\n$message")
        if (suggestion != null) append("\n\nSuggestion: $suggestion")
    }
}

fun extractLine(source: String, lineNumber: Int): String {
    val lines = source.split("\n")
    val i = lineNumber - 1
    return if (i in lines.indices) lines[i].trim() else ""
}
