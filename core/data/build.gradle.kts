plugins {
    alias(libs.plugins.deathnote.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.despicable.core.data"
}

dependencies {

    api(projects.core.model)
    api(projects.core.database)


    api(libs.javax.inject)

    implementation(libs.koin.core)

    api(libs.kotlinx.coroutines.core)

    implementation(libs.kotlinx.serialization.json)

    implementation (libs.androidx.paging.runtime.ktx)
    implementation (libs.androidx.room.paging)

}