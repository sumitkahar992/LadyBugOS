plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
}

android {
    namespace = "com.despicable.feature.detail"
}

dependencies {

    implementation(projects.core.designSystem)
    implementation(projects.core.common)
    implementation(projects.core.domain)

    // Koin
    implementation(libs.koin.androidx.compose)



}