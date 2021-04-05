import org.jetbrains.kotlin.util.findInterfaceImplementation

buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:${Versions.Protobuf.plugin}")
    }
}

allprojects {
    group = "bitspittle"

    repositories {
        jcenter()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
    }
}

plugins {
    kotlin("jvm") version Versions.Kotlin.plugin apply false
    id("org.jetbrains.compose") version Versions.Jetbrains.compose apply false
    id("com.google.protobuf") version Versions.Protobuf.plugin apply false
}
