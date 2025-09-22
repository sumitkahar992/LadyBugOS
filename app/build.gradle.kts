

plugins {
    alias(libs.plugins.deathnote.android.application)
    alias(libs.plugins.deathnote.android.application.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.about.library)
    id("kotlin-parcelize")
}

android {
    namespace = "com.despicable.ladybugos"

    sourceSets["main"].assets.srcDirs("src/main/assets", "../dummy")

    defaultConfig {
        applicationId = "com.despicable.ladybugos"
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }


    // Enable resource optimization
    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    // Create different APKs for different screen densities
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }


    dependenciesInfo {
        // Avoid Google-signed dependency metadata in builds
        // Disables dependency metadata when building APKs.
        includeInApk = false

        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.designSystem)
    implementation(projects.feature.home)
    implementation(projects.feature.detail)
    implementation(projects.feature.settings)
    implementation(projects.feature.backup)

    implementation(projects.widgets)
    implementation(projects.notifications)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material.icons.extended)



    coreLibraryDesugaring(libs.android.desugarJdkLibs)


    implementation(libs.androidx.core.splashscreen)


    // Koin
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.javax.inject)



    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    runtimeOnly(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)

    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.ui.tooling)
    debugRuntimeOnly(libs.androidx.ui.test.manifest)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.core)


}