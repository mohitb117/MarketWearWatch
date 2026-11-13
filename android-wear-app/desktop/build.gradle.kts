import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        getByName("desktopMain").dependencies {
            implementation(project(":shared"))
            implementation(project(":shared-ui"))
            implementation("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64:1.10.0")
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.12.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
        }

        getByName("desktopTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
}

tasks.register<JavaExec>("runDesktop") {
    group = "application"
    description = "Runs the MarketWear KMP Desktop application for local testing."
    val desktopCompilation = kotlin.targets.getByName("desktop").compilations.getByName("main")
    classpath(
        desktopCompilation.output.allOutputs,
        configurations.getByName("desktopRuntimeClasspath")
    )
    mainClass.set("com.marketwear.desktop.MainKt")
}
