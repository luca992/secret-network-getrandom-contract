pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("de.fayard.refreshVersions") version "0.60.5"
    }
}


dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("de.fayard.refreshVersions")
}


refreshVersions { // Optional: configure the plugin
    // ...
}


rootProject.name = "tests"

