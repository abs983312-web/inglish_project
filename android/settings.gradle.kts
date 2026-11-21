pluginManagement {
    repositories {
        google()
        // repo1 is reachable from this network; repo.maven.apache.org times out.
        maven("https://repo1.maven.org/maven2")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://plugins.gradle.org/m2")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://repo1.maven.org/maven2")
        maven("https://maven.aliyun.com/repository/central")
    }
}

rootProject.name = "Inglish"
include(":app")
