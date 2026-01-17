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
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serverWebsockets)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationJson)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
