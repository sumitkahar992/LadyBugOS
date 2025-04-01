plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
    alias(libs.plugins.about.library)
//    alias(libs.plugins.dependencyanalysis)

}

android {
    namespace = "com.despicable.feature.settings"
}

dependencies {

    api(projects.core.datastore)
    implementation(projects.feature.backup)



    implementation(libs.aboutlibrary.core)
    implementation(libs.aboutlibrary.compose)
    implementation(libs.androidx.navigation.runtime.ktx)



    coreLibraryDesugaring(libs.android.desugarJdkLibs)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)




    // Koin
    implementation(libs.koin.androidx.compose)





    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.ui.tooling)
    debugRuntimeOnly(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)

}