plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
//    alias(libs.plugins.dependencyanalysis)

}

android {
    namespace = "com.despicable.widgets"
}

dependencies {

    implementation(projects.core.data)


    // Widgets
    implementation(libs.glance.appwidget)

    implementation(libs.timber)



    // Widgets Preview
//    debugImplementation(libs.androidx.glance.preview)
//    debugImplementation(libs.androidx.glance.appwidget.preview)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)


    implementation(libs.javax.inject)


    implementation(libs.androidx.material3)



}