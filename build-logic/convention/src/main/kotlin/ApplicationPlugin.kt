import com.android.build.api.dsl.ApplicationExtension
import io.github.despicable.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 36

                signingConfigs {
                    create("release") {
                        storeFile = file("${project.rootDir}/keystore/keystore.jks")
                        storePassword = "releaseO"
                        keyAlias = "releaseO"
                        keyPassword = "releaseO"
                    }
                    getByName("debug") {
                    }
                }

                buildTypes {
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
//                        isDebuggable = false
//                        isDefault = false
//            resValue("string", "application_name", "Kot Weather")
                        signingConfig = signingConfigs.getByName("release")
                    }
                    debug {
                        isMinifyEnabled = false
                        isShrinkResources = false

                        isDebuggable = true
                        isDefault = true
//            applicationIdSuffix = ".debug"
//            resValue("string", "application_name", "Kot Weather - Debug")
                        signingConfig = signingConfigs.getByName("debug")
                    }
                }

            }
        }
    }
}


