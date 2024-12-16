plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.deathnote.android.library.compose)

}

android {
    namespace = "com.despicable.core.common"
}

dependencies {

    implementation(projects.core.model)
    implementation(projects.core.designSystem)


    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.navigation.compose)

}