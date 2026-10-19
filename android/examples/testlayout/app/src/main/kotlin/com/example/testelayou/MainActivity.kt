/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.example.testelayou

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import nativelayout.NativeView

class MainActivity : Activity() {
    private lateinit var ui: NativeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        val area = FrameLayout(this)
        root.addView(area, FrameLayout.LayoutParams(-1, -1))
        val hud = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 10f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xDD000000.toInt())
            setPadding(16, 8, 16, 8)
        }
        root.addView(hud, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)

        val bench = Bench(this, area, hud)

        ui = NativeView(
            context = this,
            area = area,
            app = { StressScreen(bench) },
            loader = { name -> runCatching { assets.open(name).bufferedReader().use { it.readText() } }.getOrNull() }
        )
    }

    override fun onDestroy() {
        ui.close()
        super.onDestroy()
    }
}
