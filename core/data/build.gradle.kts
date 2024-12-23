plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.despicable.core.data"
}

dependencies {

    api(projects.core.model)
    api(projects.core.common)
    api(projects.core.database)
    api(projects.core.datastore)

    implementation(libs.javax.inject)

    implementation(libs.koin.core)

    implementation(projects.notifications)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.kotlinx.serialization.json)

}