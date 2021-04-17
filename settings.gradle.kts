enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    // Emulate toml output, so maybe we can move this there someday
    val libs = object {
        val versions = object {
            val google = object {
                val protobuf = object {
                    val plugin = "0.8.15"
                }
            }
            val kotlin = object {
                val plugin = "1.4.32"
            }
            val desktop = object {
                val compose = "0.4.0-build179"
            }
            val java = object {
                val shadow = object {
                    val plugin = "6.1.0"
                }
            }
        }
    }

    plugins {
        kotlin("jvm") version libs.versions.kotlin.plugin
        id("org.jetbrains.compose") version libs.versions.desktop.compose
        id("com.google.protobuf") version libs.versions.google.protobuf.plugin
        id("com.github.johnrengelman.shadow") version libs.versions.java.shadow.plugin
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
include("messagebus")
include("api")
include("model")
