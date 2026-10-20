/*
 * Author: Adilson C. Rafael (@adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "io.github.getnativelayout"
version = "0.1.8"

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit:junit:4.13.2")
}

kotlin { jvmToolchain(21) }

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "nativelayout-core"
        }
    }
}
