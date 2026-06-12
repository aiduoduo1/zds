pluginManagement {
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

rootProject.name = "zds"

include(":app")
include(":jiang-common")
include(":jiang-core")
include(":jiang-arch")
include(":jiang-network")
include(":jiang-storage")
include(":jiang-ui")

project(":jiang-common").projectDir = file("../JiangCore/jiang-common")
project(":jiang-core").projectDir = file("../JiangCore/jiang-core")
project(":jiang-arch").projectDir = file("../JiangCore/jiang-arch")
project(":jiang-network").projectDir = file("../JiangCore/jiang-network")
project(":jiang-storage").projectDir = file("../JiangCore/jiang-storage")
project(":jiang-ui").projectDir = file("../JiangCore/jiang-ui")
