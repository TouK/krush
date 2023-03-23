import pl.touk.krush.gradle.signPublicationIfKeyPresent

plugins {
    kotlin("jvm") apply true
    kotlin("kapt") apply true
    kotlin("plugin.serialization") version "1.8.10"
    id("pl.allegro.tech.build.axion-release") version "1.13.14"
    `maven-publish`
}

scmVersion {
    useHighestVersion = true
    tag {
        prefix = "krush-"
    }
}

group = "pl.touk.krush"
project.version = scmVersion.version
val rootVersion = scmVersion.version

allprojects {
    repositories {
        mavenCentral()
    }
}

val snapshot = scmVersion.version.endsWith("SNAPSHOT")

configure(listOf(project(":annotation-processor"), project(":runtime"), project(":runtime-postgresql"))) {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    tasks.withType(Sign::class.java) {
        onlyIf { snapshot.not() }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "pl.touk.krush"
                artifactId = "krush-${project.name}"
                version = rootVersion

                from(components["java"])

                pom {
                    name.set("krush-${project.name}")
                    description.set("Krush, idiomatic persistence layer for Kotlin")
                    url.set("https://github.com/TouK/krush")
                    scm {
                        url.set("scm:git@github.com:TouK/krush.git")
                        connection.set("scm:git@github.com:TouK/krush.git")
                        developerConnection.set("scm:git@github.com:TouK/krush.git")
                    }
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("mateusz_sledz")
                            name.set("Mateusz Śledź")
                        }
                        developer {
                            id.set("piotr_jagielski")
                            name.set("Piotr Jagielski")
                        }
                    }
                }
                signPublicationIfKeyPresent(project)
            }
        }

        repositories {
            maven {
                credentials {
                    username = System.getenv("SONATYPE_USERNAME")
                    password = System.getenv("SONATYPE_PASSWORD")
                }
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
                url = uri(if (snapshot) snapshotsRepoUrl else releasesRepoUrl)
            }
        }
    }


}
