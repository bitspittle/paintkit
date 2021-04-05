import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:${Versions.Kotlin.cli}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Kotlin.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.Kotlin.coroutines}")
    implementation(compose.desktop.currentOs)

    implementation("com.google.protobuf:protobuf-java:${Versions.Protobuf.java}")
    implementation(project(":ipc"))
    implementation(project(":api"))

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.Test.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.Test.junit}")
}

tasks.register<Copy>("copyServerJar") {
    dependsOn(":server:fatJar")
    from(file("${project(":server").buildDir}/libs/server-fat.jar"))
    into(file("$projectDir/build/resources/main"))
    rename { fileName -> fileName.replace("server-fat.jar", "server.jar") }
}
project.tasks.getByPath("processResources").dependsOn("copyServerJar")

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}


compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "paintkit"
            packageVersion = "1.0.0"
        }
    }
}