import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm")
    id("com.google.protobuf")
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Kotlin.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.Kotlin.coroutines}")
    implementation("com.google.protobuf:protobuf-java:${Versions.Protobuf.java}")

    compileOnly("net.jcip:jcip-annotations:${Versions.Jcip.annotations}")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.Test.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.Test.junit}")
    testImplementation("com.google.truth:truth:${Versions.Test.truth}")
}

tasks.test {
    useJUnitPlatform()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}