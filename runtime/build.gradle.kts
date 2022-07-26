import pl.touk.krush.gradle.Versions

plugins {
    id("kotlin")
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    api("org.jetbrains.exposed:exposed-jdbc:${Versions.exposed}")
    api("org.jetbrains.exposed:exposed-core:${Versions.exposed}")
    api("org.jetbrains.exposed:exposed-java-time:${Versions.exposed}")
}
