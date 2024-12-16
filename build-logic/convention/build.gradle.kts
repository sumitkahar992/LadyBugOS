import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.github.despicable.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {

        register("androidApplication") {
            id = "io.despicable.android.application"
            implementationClass = "ApplicationPlugin"
        }

        register("androidApplicationCompose") {
            id = "io.despicable.android.application.compose"
            implementationClass = "ApplicationComposePlugin"
        }

        register("androidLibrary") {
            id = "io.despicable.android.library"
            implementationClass = "LibraryPlugin"
        }

        register("androidLibraryCompose") {
            id = "io.despicable.android.library.compose"
            implementationClass = "LibraryComposePlugin"
        }

        register("androidFeature") {
            id = "io.despicable.android.feature"
            implementationClass = "FeaturePlugin"
        }

        register("androidRoom") {
            id = "io.despicable.android.room"
            implementationClass = "RoomPlugin"
        }
    }
}

























































