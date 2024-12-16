plugins {
    alias(libs.plugins.deathnote.android.library)
}

android {
    namespace = "com.despicable.notifications"
}

dependencies {

    implementation(projects.core.database)


    // Koin
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Work
    implementation(libs.androidx.work.runtime.ktx)
}