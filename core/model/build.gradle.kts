plugins {
    alias(libs.plugins.deathnote.jvm.library)
    alias(libs.plugins.kotlin.serialization)

}

dependencies {
    api(libs.kotlinx.datetime)

    implementation(libs.kotlinx.serialization.json)


}