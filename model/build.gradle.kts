plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.bundles.test.support)
    testRuntimeOnly(libs.junit.engine)
}
