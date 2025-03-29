plugins {
    alias(libs.plugins.deathnote.android.feature)
    alias(libs.plugins.deathnote.android.library.compose)
    alias(libs.plugins.about.library)
}

android {
    namespace = "com.despicable.feature.settings"
}

dependencies {

    api(projects.core.datastore)
    api(projects.feature.backup)



    implementation(libs.aboutlibrary.core)
    implementation(libs.aboutlibrary.compose)
    implementation(libs.androidx.navigation.runtime.ktx)



    coreLibraryDesugaring(libs.android.desugarJdkLibs)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)




    // Koin
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core)

    implementation(libs.javax.inject)


    // Work
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.androidx.workmanager)


    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}