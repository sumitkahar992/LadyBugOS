plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
    alias(libs.plugins.about.library)
}

android {
    namespace = "com.despicable.feature.settings"
}

dependencies {

    api(projects.core.model)
    api(projects.core.datastore)
    api(projects.core.designSystem)
    api(projects.core.common)
    api(projects.feature.backup)

    implementation(libs.aboutlibrary.core)
    implementation(libs.aboutlibrary.compose)

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.material3)

    // Koin
    implementation(libs.koin.androidx.compose)

}