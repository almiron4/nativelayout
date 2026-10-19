/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.example.testelayou

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import nativelayout.NativeView

class Task(var title: String, var done: Boolean)

class MainActivity : Activity() {
    private lateinit var ui: NativeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val area = FrameLayout(this)
        setContentView(area)

        ui = NativeView(
            this,
            area,
            { name -> runCatching { assets.open(name).bufferedReader().use { it.readText() } }.getOrNull() }
        ) {
            val screen = screen("tasks.nl")

            val tasks = mutableListOf<Task>()
            if (exists("tasks")) {
                load("tasks").orEmpty().lines()
                    .filter { it.length > 1 }
                    .forEach { tasks.add(Task(it.substring(1), it[0] == '1')) }
            } else {
                tasks.add(Task("Test nativelayout on a real device", true))
                tasks.add(Task("Write the Android guide", false))
                tasks.add(Task("Publish the library", false))
            }

            var filter = "all"

            fun persist() {
                save("tasks", tasks.joinToString("\n") { (if (it.done) "1" else "0") + it.title })
            }

            while (true) {
                val shown = when (filter) {
                    "pending" -> tasks.filter { !it.done }
                    "done" -> tasks.filter { it.done }
                    else -> tasks.toList()
                }
                val total = tasks.size
                val done = tasks.count { it.done }
                val empty = when (filter) {
                    "pending" -> "Nothing pending. Nice work."
                    "done" -> "Nothing completed yet."
                    else -> "No tasks yet. Add the first one above."
                }

                val data = mapOf(
                    "total" to total,
                    "done" to done,
                    "pending" to (total - done),
                    "percent" to (if (total == 0) 0 else done * 100 / total),
                    "filter" to filter,
                    "empty" to empty,
                    "shown" to shown.map { mapOf("title" to it.title, "done" to it.done) }
                )

                if (!render(screen, data)) break

                if (click("add")) {
                    val title = value("title").trim()
                    if (title.isNotEmpty()) {
                        tasks.add(Task(title, false))
                        setValue("title", "")
                        persist()
                    }
                }
                if (click("show-all")) filter = "all"
                if (click("show-pending")) filter = "pending"
                if (click("show-done")) filter = "done"
                if (click("clear")) {
                    tasks.removeAll { it.done }
                    persist()
                }
                if (click(".card")) {
                    shown.getOrNull(clickIndex())?.let { it.done = !it.done }
                    persist()
                }
                if (click(".remove")) {
                    shown.getOrNull(clickIndex())?.let { tasks.remove(it) }
                    persist()
                }
            }
        }
    }

    override fun onDestroy() {
        ui.close()
        super.onDestroy()
    }
}
