/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nativelayout"
include(":core", ":android", ":app")
