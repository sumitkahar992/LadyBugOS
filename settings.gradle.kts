enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LadyBugOS"

include(":app")


include(":notifications")
include(":widgets")


include(":core:model")
include(":core:data")
include(":core:domain")
include(":core:database")
include(":core:common")
include(":core:design-system")
include(":core:datastore")



include(":feature:home")
include(":feature:detail")
include(":feature:settings")
include(":feature:backup")
