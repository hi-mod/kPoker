plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    application
}

group = "com.poker.server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerWebsockets)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktor.serialization)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorServerDefaultHeaders)
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerAuthJwt)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerSwagger)
    implementation(libs.ktorServerAuth)

    implementation(libs.logbackClassic)

    implementation(project(":common"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
    mainClass.set("com.poker.server.application.ServerKt")
}

tasks.test {
    useJUnitPlatform()
}