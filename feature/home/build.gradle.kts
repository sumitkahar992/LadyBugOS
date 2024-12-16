plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
}

android {
    namespace = "com.despicable.feature.home"
}

dependencies {

    implementation(projects.core.designSystem)
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.domain)
    implementation(projects.widgets)

    // Koin
    implementation(libs.koin.androidx.compose)
}