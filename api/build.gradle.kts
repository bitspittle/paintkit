plugins {
    id("java")
    kotlin("jvm")
    id("com.google.protobuf")
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.google.protobuf.java)
    implementation(project(":model"))
}
