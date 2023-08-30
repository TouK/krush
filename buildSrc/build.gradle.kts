repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    gradleApi()
    implementation("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.jvm.gradle.plugin", "1.9.10")
    implementation("io.github.gradle-nexus", "publish-plugin", "1.0.0")
}

plugins {
    `kotlin-dsl` apply true
}
