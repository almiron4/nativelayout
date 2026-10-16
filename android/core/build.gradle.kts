/*
 * Author: Adilson C. Rafael (@adilson889)
 * Date: 2026-10-15
 */

plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "io.github.getnativelayout"
version = "0.1.0"

dependencies {
    implementation(kotlin("stdlib"))
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
