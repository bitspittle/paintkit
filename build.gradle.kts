import org.jetbrains.kotlin.util.findInterfaceImplementation

buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.15")
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
    kotlin("jvm") apply false
    id("org.jetbrains.compose") apply false
    id("com.google.protobuf") apply false
    id("com.github.johnrengelman.shadow") apply false
}
