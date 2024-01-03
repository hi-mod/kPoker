pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            google()
            gradlePluginPortal()
            mavenCentral()
/*
            maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
            maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
            maven("https://androidx.dev/storage/compose-compiler/repository")
            maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
*/
        }
    }
}

rootProject.name = "poker"
include(":common")
include(":poker-client-desktop")
include(":poker-server")

