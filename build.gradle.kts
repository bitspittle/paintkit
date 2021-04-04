allprojects {
    ext {
        group = "bitspittle"
    }

    repositories {
        jcenter()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.compose") apply false
}
