/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

android {
    namespace = "co.adilson889.nativelayout.android"
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
