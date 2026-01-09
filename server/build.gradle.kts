import com.google.cloud.tools.gradle.appengine.appyaml.AppEngineAppYamlExtension

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.gradleup.shadow)
    alias(libs.plugins.google.gcloud.tools.appengine)
    application
}

group = "com.aaronchancey.poker"
version = "1.0.0"
application {
    mainClass.set("com.aaronchancey.poker.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.kotlinx.serializationJson)

    implementation(projects.shared)
    implementation(projects.kPoker)

    implementation(libs.logback)

    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverWebsockets)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationJson)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

configure<AppEngineAppYamlExtension> {
    stage {
        setArtifact("build/libs/${project.name}-all.jar")
    }
    deploy {
        version = "GCLOUD_CONFIG"
        projectId = "GCLOUD_CONFIG"
    }
}

val wasmDistDir = project.rootDir.resolve("composeApp/build/dist/wasmJs/productionExecutable")

tasks.named<ProcessResources>("processResources") {
    // Only depend on wasmJs task if it exists (when building locally with Android SDK)
    // For Cloud Build, Wasm is pre-built and included in the upload
    val wasmTask = tasks.findByPath(":composeApp:wasmJsBrowserDistribution")
    if (wasmTask != null) {
        dependsOn(wasmTask)
    }
    from(wasmDistDir) {
        into("static")
    }
}
