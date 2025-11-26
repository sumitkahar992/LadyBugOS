plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
}

android {
    namespace = "com.despicable.feature.backup"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)

    // Koin
    implementation(libs.koin.androidx.compose)
}