plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.deathnote.android.room)
    alias(libs.plugins.kotlin.serialization)
//    alias(libs.plugins.dependencyanalysis)
}

android {
    namespace = "com.despicable.core.database"
}

dependencies {


    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.serialization.json)


    implementation(libs.javax.inject)

    implementation(libs.kotlinx.datetime)





}