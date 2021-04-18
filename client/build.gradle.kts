import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlin.cli)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(compose.desktop.currentOs)

    implementation(libs.google.protobuf.java)
    implementation(project(":ipc"))
    implementation(project(":api"))
    implementation(project(":model"))

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.bundles.test.support)
    testRuntimeOnly(libs.junit.engine)
}

tasks.register<Copy>("copyServerJar") {
    dependsOn(":server:shadowJar")
    from(file("${project(":server").buildDir}/libs/server-all.jar"))
    into(file("$projectDir/build/resources/main"))
    rename("server-all.jar", "server.jar")
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