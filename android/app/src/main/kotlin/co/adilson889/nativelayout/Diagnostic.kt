/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
