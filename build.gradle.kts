import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta15"
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.stephengold:Libbulletjme-Windows64:22.0.1")

    // native libraries:
    runtimeOnly("com.github.stephengold:Libbulletjme-Linux64:22.0.1:SpDebug")

    implementation("io.github.electrostat-lab:snaploader:1.0.0-stable")

    implementation("net.minestom:minestom-snapshots:1_21_5-69b9a5d844")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("BlockPhysics")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "dev.emortal.Main"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}