plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.deathnote.android.library.compose)
//    alias(libs.plugins.dependencyanalysis)


}

android {
    namespace = "com.despicable.core.designsystem"
}

dependencies {

    api(projects.core.model)

    implementation(libs.material)
    api(platform(libs.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.material3)
    api(libs.androidx.compose.material.icons.extended)


    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)


    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    debugImplementation(libs.ui.tooling)
    debugRuntimeOnly(libs.androidx.ui.test.manifest)




}





















