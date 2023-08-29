import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
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
    maven("https://repo.unnamed.team/repository/unnamed-public/")
}

dependencies {
    testImplementation(kotlin("test"))

    // Minestom
    implementation("com.github.bea4dev:minestom-ce:6f7bbdee57")

    // Log
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")

    // Kotlin-result
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")

    // Toml
    implementation("cc.ekblad:4koma:1.2.0")

    // Entity model
    implementation("com.github.bea4dev:hephaestus-engine:4dc6c24b2f")

    // Console
    implementation("org.jline:jline-terminal-jansi:3.21.0")
    implementation("net.minecrell:terminalconsoleappender:1.3.0")
    implementation("org.jline:jline-terminal:3.9.0")
    implementation("org.jline:jline-reader:3.9.0")
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

tasks.shadowJar {
    transform(Log4j2PluginsCacheFileTransformer())
}

tasks.jar {
    manifest {
        attributes("Multi-Release" to true)
    }

    from("src/main/resources") {
        include("**/*.*")
    }

    manifest {
        attributes["Main-Class"] = "com.github.bea4dev.vanilla_source.MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}