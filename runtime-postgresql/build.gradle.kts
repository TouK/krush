import pl.touk.krush.gradle.Versions

plugins {
    id("kotlin")
}

dependencies {
    api(project(":runtime"))
    implementation("org.postgresql:postgresql:${Versions.postgresDriver}")
}
