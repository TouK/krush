import pl.touk.krush.gradle.Versions

plugins {
    id("kotlin")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    implementation("com.squareup:kotlinpoet:${Versions.kotlinpoet}")
    implementation("com.squareup:kotlinpoet-metadata:${Versions.kotlinpoet}")
    implementation("com.squareup:kotlinpoet-ksp:${Versions.kotlinpoet}")
    implementation("com.google.devtools.ksp:symbol-processing-api:${Versions.ksp}")

    api("org.hibernate.javax.persistence:hibernate-jpa-2.1-api:1.0.2.Final")

    api(project(":runtime"))

    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junit}")
    testImplementation("org.assertj:assertj-core:${Versions.assertj}")
    testRuntimeOnly("ch.qos.logback:logback-classic:${Versions.logback}")
    testImplementation("io.github.jbock-java:compile-testing:0.19.12")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
