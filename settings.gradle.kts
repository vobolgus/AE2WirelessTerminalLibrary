rootProject.name = "ae2wtlib"
pluginManagement {
    // The Fabric loom plugin marker lives on maven.fabricmc.net, not on the Gradle Plugin Portal.
    // Declaring `repositories` here replaces the implicit default, so the portal has to be re-added.
    repositories {
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
    plugins {
        id("net.neoforged.moddev") version "2.0.141"
        id("net.neoforged.moddev.repositories") version "2.0.141"
        id("com.diffplug.spotless") version "7.0.0.BETA2"
        // https://fabricmc.net/develop/ - loom 1.17.x requires Gradle >= 9.5 (wrapper bumped to 9.5.1)
        id("net.fabricmc.fabric-loom") version "1.17.8"
    }
}
plugins {
    id("net.neoforged.moddev.repositories")
}
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://modmaven.dev/")
            content {
                includeGroup("mezz.jei")
            }
        }
        maven {
            url = uri("https://maven.shedaniel.me/")
            content {
                includeGroup("me.shedaniel")
                includeGroup("me.shedaniel.cloth")
                includeGroup("dev.architectury")
            }
        }
        maven {
            url = uri("https://maven.terraformersmc.com/")
            content {
                includeGroup("dev.emi")
            }
        }
        maven {
            url = uri("https://maven.theillusivec4.top/")
            content {
                includeGroup("top.theillusivec4.curios")
            }
        }
        maven {
            url = uri("https://api.modrinth.com/maven")
            content {
                includeGroup("maven.modrinth")
            }
        }
    }
}

include("ae2wtlib_api")

// Fabric loader project (the port). loom resolves its dependencies at CONFIGURATION time, so CI jobs
// that only build NeoForge can drop the whole subproject with -Pae2wtlib.skipFabric=true.
// Mirrors the `-Pae2.skipFabric` switch in the Applied-Energistics-2 fork.
val skipFabric = providers.gradleProperty("ae2wtlib.skipFabric").getOrElse("false") == "true"
if (!skipFabric) {
    include("loader:fabric")
}
