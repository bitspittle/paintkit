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
    implementation("com.google.protobuf:protobuf-java:${Versions.Protobuf.java}")
}
