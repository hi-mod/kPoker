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