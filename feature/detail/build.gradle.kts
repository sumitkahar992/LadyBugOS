plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
//    alias(libs.plugins.dependencyanalysis)

}

android {
    namespace = "com.despicable.feature.detail"
}

dependencies {

    api(projects.core.domain)
    api(projects.core.data)

    api(projects.widgets)


    implementation(libs.androidx.navigation.runtime.ktx)

    implementation(libs.timber)


    coreLibraryDesugaring(libs.android.desugarJdkLibs)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)




    // Koin
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core.viewmodel)

//    implementation(libs.koin.core)




    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)


    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.ui.tooling)
    debugRuntimeOnly(libs.androidx.ui.test.manifest)
    implementation(libs.reorderable)




}