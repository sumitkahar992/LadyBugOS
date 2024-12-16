plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.deathnote.android.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.despicable.database"
}

dependencies {


    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)

    implementation(libs.javax.inject)

    implementation(libs.kotlinx.datetime)

    implementation(libs.kotlinx.serialization.json)


}