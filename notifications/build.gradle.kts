plugins {
    alias(libs.plugins.deathnote.android.library)
}

android {
    namespace = "com.despicable.notifications"
}

dependencies {

    implementation(projects.core.data)
    implementation(projects.core.model)


    // Koin
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Work
    implementation(libs.androidx.work.runtime.ktx)

}