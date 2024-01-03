plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
}

group = "com.poker.server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    jvm()

    sourceSets {
        commonMain.dependencies {
            // Works as common dependency as well as the platform one
            implementation(libs.kotlinx.serialization)

            implementation(libs.ktor.client.webSockets)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.json)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.client.logging)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
            implementation(libs.logbackClassic)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
