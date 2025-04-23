plugins {
    alias(libs.plugins.deathnote.android.library)
}

android {
    namespace = "com.despicable.core.datastore"
}

dependencies {

    api(projects.core.model)
    api(projects.core.designSystem)
    //
    api(projects.core.data)


    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    // DataStore
    implementation(libs.androidx.dataStore.preferences)
    implementation(libs.javax.inject)

}