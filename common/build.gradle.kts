@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    alias(libs.plugins.kotest.multiplatform)
}

group = "com.poker.server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(18)

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
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.framework.datatest)
            implementation(libs.kotest.property)
            implementation(libs.mockk)
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5.jvm)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
