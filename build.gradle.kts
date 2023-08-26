import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

group = "com.github.bea4dev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    // Minestom
    implementation("dev.hollowcube:minestom-ce:5ba3d92d83")

    // Logback
    implementation("ch.qos.logback:logback-core:1.4.11")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Kotlin-result
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")

    // Log for stdout
    implementation("uk.org.lidalia:sysout-over-slf4j:1.0.2")

    // Toml
    implementation("cc.ekblad:4koma:1.2.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("com.github.bea4dev.vanilla_source.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.github.bea4dev.vanilla_source.MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}