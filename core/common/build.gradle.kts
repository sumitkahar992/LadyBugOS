plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.deathnote.android.library.compose)
//    alias(libs.plugins.dependencyanalysis)


}

android {
    namespace = "com.despicable.core.common"
}

dependencies {



    api(libs.androidx.animation.core)




    coreLibraryDesugaring(libs.android.desugarJdkLibs)


    implementation(libs.androidx.navigation.compose)




    implementation(libs.androidx.compose.foundation)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    debugImplementation(libs.ui.tooling)
    debugRuntimeOnly(libs.androidx.ui.test.manifest)



}