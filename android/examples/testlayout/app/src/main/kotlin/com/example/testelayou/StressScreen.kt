/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.example.testelayou

import nativelayout.NativeView
import nativelayout.Page

class StressState {
    var first = true
    var n = 100
    var qtd = IntArray(100)
}

fun NativeView.StressScreen(bench: Bench): Page {
    val s = state { StressState() }

    var changed = s.first
    var label = "abrir N=${s.n}"
    s.first = false

    when {
        click(".chip") -> {
            s.n = sizes[clickIndex()]
            s.qtd = IntArray(s.n)
            changed = true
            label = "trocar para N=${s.n}"
        }
        click(".mais") -> {
            s.qtd[clickIndex()]++
            changed = true
            label = "+1 num item"
        }
        click("mudar") -> {
            s.qtd[s.n / 2]++
            changed = true
            label = "mudar o item do meio"
        }
        click("rolar") -> bench.fpsScroll()
        click("bench") -> Thread { bench.benchCore() }.start()
    }

    if (changed) bench.armDraw(label)
    return view("stress.nl", stressData(s.n, s.qtd))
}
