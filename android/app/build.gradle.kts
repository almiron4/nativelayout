/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "co.adilson889.nativelayout"
    compileSdk = 36

    defaultConfig {
        applicationId = "co.adilson889.nativelayout"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core"))
    implementation(project(":android"))
}
