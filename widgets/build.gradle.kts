plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
}

android {
    namespace = "com.despicable.widgets"
}

dependencies {

    implementation(projects.core.model)
    implementation(projects.core.domain)


    // Widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.androidx.core.splashscreen)

    // Widgets Preview
    debugImplementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidget.preview)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)


    implementation(libs.javax.inject)

    implementation(libs.androidx.workmanager)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.material3)

}