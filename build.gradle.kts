import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("maven-publish")
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
    implementation("com.github.bea4dev:Minestom:7593b2c83c")

    // Log
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")

    // Kotlin-result
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")

    // Toml
    implementation("cc.ekblad:4koma:1.2.0")

    // Entity model
    implementation("com.github.bea4dev:hephaestus-engine:0e238980aa")

    // Console
    implementation("org.jline:jline-terminal-jansi:3.21.0")
    implementation("net.minecrell:terminalconsoleappender:1.3.0")
    implementation("org.jline:jline-terminal:3.9.0")
    implementation("org.jline:jline-reader:3.9.0")

    // Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-minestom-api:2.13.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-minestom-core:2.13.0")

    // Script
    implementation("com.github.bea4dev:Contan:0cb1c15c65")

    // For native libs
    implementation("org.apache.commons:commons-lang3:3.13.0")

    // Misc
    implementation("com.google.guava:guava:33.0.0-jre")

    // Toml
    implementation("com.moandjiezana.toml:toml4j:0.7.2")

    // Yaml
    implementation("org.yaml:snakeyaml:2.2")

    // Noise
    implementation("de.articdive:jnoise-pipeline:4.1.0")

    implementation(kotlin("reflect"))

    implementation("com.github.AtlasEngineCa:WorldSeedEntityEngine:a90016c386")
    implementation("commons-io:commons-io:2.11.0")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["java"])
                groupId = "com.github.bea4dev"
                artifactId = "VanillaSource"
                version = "1.0-SNAPSHOT"
            }
        }
    }
}