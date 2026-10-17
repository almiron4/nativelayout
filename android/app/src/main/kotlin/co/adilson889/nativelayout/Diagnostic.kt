/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout

enum class Severity { WARNING, ERROR }

data class Diagnostic(
    val line: Int,
    val severity: Severity,
    val message: String,
    val start: Int = -1,
    val end: Int = -1,
    val suggestion: String? = null
) {
    val isWord: Boolean get() = start >= 0 && end > start
}
