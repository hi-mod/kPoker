plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.0-alpha01"
    application
}

group = "com.poker.client.desktop"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.ui)
    implementation(libs.kotlinx.coroutines.swing)

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.webSockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.json)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)

    implementation(libs.multiplatform.settings)

    implementation(project(":common"))
}

application {
    mainClass.set("com.poker.client.desktop.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(18)
}
