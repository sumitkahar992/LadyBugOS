plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.despicable.core.domain"
}

dependencies {

    api(projects.core.model)
    api(projects.core.data)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)

    implementation(libs.kotlinx.serialization.json)


    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)
}