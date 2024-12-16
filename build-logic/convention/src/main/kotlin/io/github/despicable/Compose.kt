package io.github.despicable

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        dependencies {
            val composeBom = libs.findLibrary("compose-bom").get()
            val koinBom = libs.findLibrary("koin-bom").get()

            add("implementation", platform(composeBom))
            add("androidTestImplementation", platform(composeBom))

            add("implementation", platform(koinBom))


            add("implementation", libs.findLibrary("ui-tooling-preview").get())
            add("debugImplementation", libs.findLibrary("ui-tooling").get())
        }
    }
}
