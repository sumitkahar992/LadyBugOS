plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.deathnote.android.library.compose)

}

android {
    namespace = "com.despicable.core.designsystem"
}

dependencies {

    api(projects.core.model)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    api(platform(libs.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.material3)
    api(libs.androidx.compose.material.icons.extended)

    api(libs.coil.kt.compose)


}





















