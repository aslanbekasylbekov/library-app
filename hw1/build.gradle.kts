plugins {
    kotlin("jvm") version "2.3.0"
    application
}

group = "library"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

application {
    mainClass.set("library.MainKt")
}
