plugins {
    java
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlin.cli)
    implementation(libs.bundles.kotlin.coroutines)

    implementation(libs.google.protobuf.java)
    implementation(project(":ipc"))
    implementation(project(":api"))
}

val mainClass = "MainKt"

project.setProperty("mainClassName", mainClass)
application {
    mainClass.set("MainKt")
}
