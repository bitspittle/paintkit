pluginManagement {
    plugins {
        val versionKotlin: String by settings
        kotlin("jvm") version versionKotlin apply false

        val versionCompose: String by settings
        id("org.jetbrains.compose") version versionCompose apply false
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }

}
rootProject.name = "paintkit"
include("client")
include("server")
include("protocol")
include("model")
