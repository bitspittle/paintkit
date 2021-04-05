pluginManagement {
    plugins {
        kotlin("jvm") version "1.4.30"
        id("org.jetbrains.compose") version "1.4.3"
        id("com.google.protobuf") version "0.8.15"
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
include("ipc")
include("api")
include("model")
