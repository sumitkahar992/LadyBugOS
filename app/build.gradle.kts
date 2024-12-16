plugins {
    alias(libs.plugins.deathnote.android.application)
    alias(libs.plugins.deathnote.android.application.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.about.library)
    alias(libs.plugins.deathnote.android.room)
    id("kotlin-parcelize")
}

android {
    namespace = "com.despicable.ladybugos"

    sourceSets {
        getByName("main") {
            assets.srcDirs(listOf("src/main/assets", "../dummy"))
        }
    }
//    sourceSets["main"].assets.srcDirs("src/main/assets", "../dummy")


    defaultConfig {
        applicationId = "com.despicable.ladybugos"
        versionCode = 1
        versionName = "1.0"
    }

    dependenciesInfo {
        // Avoid Google-signed dependency metadata in builds
        includeInApk = false
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
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.domain)
    implementation(projects.core.designSystem)
    implementation(projects.feature.settings)
    implementation(projects.feature.home)
    implementation(projects.feature.detail)

    implementation(projects.widgets)
    implementation(projects.notifications)

    implementation(libs.androidx.compose.animation)

    implementation(libs.aboutlibrary.core)
    implementation(libs.aboutlibrary.compose)

    implementation(libs.androidx.compose.material.icons.extended)

    // Widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.androidx.core.splashscreen)

    // Widgets Preview
    debugImplementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidget.preview)


    // Koin
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.javax.inject)

    // DataStore
    implementation(libs.androidx.dataStore.preferences)

    // Work
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.koin.androidx.compose)


    implementation(libs.timber)


    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)


}