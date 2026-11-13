import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "MarketWearSharedUi"
            isStatic = true
        }
    }

    js {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            api("org.jetbrains.compose.runtime:runtime:1.12.0")
            api("org.jetbrains.compose.foundation:foundation:1.12.0")
            api("org.jetbrains.compose.material3:material3:1.9.0")
            api("org.jetbrains.compose.ui:ui:1.12.0")
            api("org.jetbrains.compose.ui:ui-tooling-preview:1.12.0")
            api(libs.vico.compose)
            api(libs.vico.compose.m3)
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
}

android {
    namespace = "com.marketwear.sharedui"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
