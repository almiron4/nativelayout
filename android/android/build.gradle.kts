/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

android {
    namespace = "nativelayout"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    api(project(":core"))
}

group = "io.github.getnativelayout"
version = "0.1.0"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "nativelayout-android"
            }
        }
    }
}
