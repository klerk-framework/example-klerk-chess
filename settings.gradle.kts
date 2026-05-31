rootProject.name = "klerk-chess"
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral() {
            content {
                excludeGroup("dev.klerkframework")
            }
        }
        maven ("https://jitpack.io") {
            content {
                includeGroup("dev.klerkframework")
            }
        }
    }
}
