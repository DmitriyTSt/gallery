import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "ru.dmitriyt"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.imgscalr:imgscalr-lib:4.2")
    implementation("com.drewnoakes:metadata-extractor:2.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Gallery"
            packageVersion = "1.0.0"
            description = "DmitriyT's gallery app"

            macOS {
                iconFile.set(project.file("src/main/resources/gallery.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/gallery.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/gallery.png"))
            }
        }
    }
}