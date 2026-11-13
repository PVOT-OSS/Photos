// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    includeBuild("PvotLib/build-logic")
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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Photos"

// PvotLib is a submodule so it can change alongside the app; its modules replace the Maven ones.
includeBuild("PvotLib") {
    dependencySubstitution {
        substitute(module("com.prauga.pvot:design-system")).using(project(":design-system"))
        substitute(module("com.prauga.pvot:core-ui")).using(project(":core-ui"))
    }
}

include(":app")
