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
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

/*configure<AppEngineAppYamlExtension> {
    stage {
        setArtifact("build/libs/${project.name}-all.jar")
    }
    deploy {
        version = "GCLOUD_CONFIG"
        projectId = "GCLOUD_CONFIG"
    }
}*/

// Check for WASM_DEBUG env var.
// Default to production for safety/deployments.
/*val isWasmDebug = System.getenv("WASM_DEBUG") == "true"
val skipClientBuild = project.hasProperty("skipClientBuild")

if (!skipClientBuild) {
    println("WASM Debug Mode: $isWasmDebug")
    // Correct task names based on 'gradle :composeApp:tasks' output
    val wasmSimpleTaskName = if (isWasmDebug) "wasmJsBrowserDevelopmentExecutableDistribution" else "wasmJsBrowserDistribution"
    val wasmDistPath = if (isWasmDebug) "composeApp/build/dist/wasmJs/developmentExecutable" else "composeApp/build/dist/wasmJs/productionExecutable"
    val wasmDistDir = project.rootDir.resolve(wasmDistPath)

    val composeAppProject = project(":composeApp")
    val wasmTaskProvider = composeAppProject.tasks.named(wasmSimpleTaskName)

    tasks.named<ProcessResources>("processResources") {
        dependsOn(wasmTaskProvider)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(wasmDistDir) {
            into("static")
        }
    }
}*/
