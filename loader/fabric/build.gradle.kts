/*
 * Fabric loader project for AE2WTLib (MC 26.1.2).
 *
 * Follows the dual-loader recipe proven on the Applied-Energistics-2 / GuideME forks
 * (see ~/IdeaProjects/FABRIC_PORTING_PLAYBOOK.md Part 8):
 *
 *   - the SHARED sources stay at the repository root (`src/main/java`) and in `ae2wtlib_api`
 *     (`ae2wtlib_api/src/main/java`); this project pulls them in as extra srcDirs,
 *   - the Fabric-only platform layer lives in `loader/fabric/src/main/java`,
 *   - the NeoForge projects (root + `:ae2wtlib_api`) are untouched and stay green at every commit,
 *     acting as the regression harness and keeping the fork upstream-rebaseable.
 *
 * W1 STATUS: the shared sources still import net.neoforged.* wholesale (19 of 103 files), so they
 * are OFF by default. Flip them on per tree once the W2/W3 decoupling seams land:
 *
 *     -Pae2wtlib.fabric.api=true      # ae2wtlib_api/src/main/java
 *     -Pae2wtlib.fabric.shared=true   # src/main/java
 *
 * Unlike NeoForge (where `ae2wtlib_api` is a separate jarJar'd library jar), Fabric builds ONE jar
 * out of both trees - same call as the AE2 fork's single-jar arrangement. If the API ever needs to
 * ship separately for addons, split it with `include()` (jar-in-jar) later.
 */

plugins {
    id("net.fabricmc.fabric-loom")
    id("com.diffplug.spotless")
}

val mavenGroup: String by project
val modID: String by project
val ae2wtlibCurrentMajor: String by project
val ae2Version: String by project

group = mavenGroup
version = "${ae2wtlibCurrentMajor}0.0.0-SNAPSHOT"

System.getenv("PR_NUMBER")?.takeIf { it.isNotEmpty() }?.let { version = "0.0.0-pr$it" }
System.getenv("TAG")?.takeIf { it.isNotEmpty() }?.let { version = it }

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

fun prop(name: String): String = providers.gradleProperty(name).get()

// fabric-loom injects project-level repositories; under the default PREFER_PROJECT mode that SHADOWS
// the settings-level repository list for this project. Declare the full set we need here.
// (playbook Part 2 - this exact trap cost a day on an earlier port.)
repositories {
    mavenLocal {
        content {
            // Our AE2 fork's Fabric build + the GuideME Fabric port. Neither is on a public maven.
            includeModule("org.appliedenergistics", "appliedenergistics2-fabric")
            includeModule("org.appliedenergistics", "guideme-fabric")
        }
    }
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "MojangLibraries"
        url = uri("https://libraries.minecraft.net/")
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
        url = uri("https://maven.blamejared.com/")
        content {
            includeGroup("mezz.jei")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    mavenCentral()
}

base {
    archivesName = "ae2wtlib-fabric"
}

loom {
    // NOTE: splitEnvironmentSourceSets() is deliberately NOT used yet. The shared tree mixes client
    // and server classes exactly like the NeoForge jar does (AE2wtlib.registerScreens, the Gui mixin,
    // ...), and the AE2 fork settled on the same single-source-set arrangement. The dedicated
    // `runServer` boot is the client-class-leak gate instead (playbook golden rule #3).
    mods {
        create("ae2wtlib") {
            sourceSet(sourceSets["main"])
        }
    }

    accessWidenerPath = file("src/main/resources/ae2wtlib.accesswidener")

    runs {
        configureEach {
            runDir = "run"
        }
        named("server") {
            programArgs("nogui")
        }
    }
}

// ---------------------------------------------------------------------------
// Shared-source gates.
//   W1: both OFF - the shared trees were still NeoForge-coupled and only the skeleton had to build.
//   W2 onwards: both ON by default. The seams landed and the Fabric platform layer now *references* the shared
//   trees, so a build with them off no longer compiles. The switches are kept (they still accept
//   -Pae2wtlib.fabric.api=true / -Pae2wtlib.fabric.shared=true, and =false for a deliberate bisect) so the W1
//   command crib and any CI job that still passes them keeps working.
// ---------------------------------------------------------------------------
val includeSharedApiSources = providers.gradleProperty("ae2wtlib.fabric.api").getOrElse("true") == "true"
val includeSharedSources = providers.gradleProperty("ae2wtlib.fabric.shared").getOrElse("true") == "true"

sourceSets {
    named("main") {
        if (includeSharedApiSources) {
            java.srcDir(rootProject.file("ae2wtlib_api/src/main/java"))
            resources.srcDir(rootProject.file("ae2wtlib_api/src/main/resources"))
        }
        if (includeSharedSources) {
            java.srcDir(rootProject.file("src/main/java"))
            resources.srcDir(rootProject.file("src/main/resources"))
        }
        java {
            // ---------------------------------------------------------------
            // NeoForge overlay - the loader-specific halves of the shared seams. These live in the shared tree
            // (so the NeoForge build stays a plain single-module build) but must never reach the Fabric compile.
            // ---------------------------------------------------------------
            exclude("de/mari_023/ae2wtlib/neoforge/**")
            exclude("de/mari_023/ae2wtlib/api/AE2wtlibAPIEntrypoint.java")

            // ---------------------------------------------------------------
            // The two NeoForge @Mod classes. PERMANENTLY excluded - they ARE the NeoForge overlay now.
            // W3 re-homed their loader-neutral bodies into the shared AE2wtlibClientEvents plus the two Fabric
            // entrypoints and the Fabric mixin package.
            // ---------------------------------------------------------------
            exclude("de/mari_023/ae2wtlib/AE2wtlibForge.java")
            exclude("de/mari_023/ae2wtlib/AE2wtlibClient.java")

            // ---------------------------------------------------------------
            // W6 - recipe viewer plugins (JEI/REI entrypoints on Fabric; EMI has no 26.1 Fabric artifact).
            // ---------------------------------------------------------------
            exclude("de/mari_023/ae2wtlib/recipeviewer/**")

            // ---------------------------------------------------------------
            // W3 (R4) - the one shared mixin that is genuinely NeoForge-only: ServerPlayerMixin captures
            // @Local(name = "selected") out of ServerPlayer#drop(Z), a local that only exists in NeoForge's PATCHED
            // vanilla (javap: vanilla has `removed` in slot 3 and no `selected` at all, and `removed` is the
            // split-off copy handed to the dropped ItemEntity - semantically the wrong stack). The Fabric twin is
            // de.mari_023.ae2wtlib.fabric.mixin.ServerPlayerDropMixin; see its javadoc.
            // ---------------------------------------------------------------
            exclude("de/mari_023/ae2wtlib/mixin/ServerPlayerMixin.java")
        }
        resources {
            // NeoForge metadata must never ship in the Fabric jar.
            exclude("META-INF/neoforge.mods.toml")
            exclude("META-INF/accesstransformer.cfg")
            // The NeoForge mixin configs are replaced by this project's `*.fabric.mixins.json` copies
            // (deliberately different file names so this exclude cannot swallow our own copies).
            exclude("ae2wtlib.mixins.json")
            exclude("ae2wtlib_api.mixins.json")
            // Curios is a NeoForge-only mod; its item tag has no meaning in the Fabric jar
            // (a Trinkets equivalent would live under data/trinkets/ - see PORTING_NOTES section 5).
            exclude("data/curios/**")
        }
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:${prop("minecraft_version")}")
    // MC 26.1+ is unobfuscated: NO `mappings` line, and loom 1.17 dropped the `mod*` remapping
    // configurations - everything is a plain `implementation` (playbook Part 2).

    implementation("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")

    // Our AE2 Fabric fork. Hand-installed into mavenLocal for now:
    //   ~/.m2/repository/org/appliedenergistics/appliedenergistics2-fabric/26.1.10-beta/
    // The durable fix is adding `maven-publish` to the AE2 fork's loader/fabric/build.gradle.kts and
    // running `./gradlew :fabric:publishToMavenLocal` - see PORTING_NOTES.md "AE2 dependency".
    implementation("org.appliedenergistics:appliedenergistics2-fabric:$ae2Version")
    // AE2's client classes (AEBaseScreen and friends) reference GuideME types in their signatures.
    compileOnly("org.appliedenergistics:guideme-fabric:${prop("guideme_fabric_version")}")

    // night-config backs the Fabric config store (the twin of NeoForge's ModConfigSpec). AE2's Fabric jar
    // already jar-in-jars the same coordinates; loader de-duplicates nested jars, and shipping our own keeps
    // this jar self-contained instead of depending on another mod's nested libraries.
    implementation("com.electronwill.night-config:toml:${prop("night_config_version")}")
    "include"("com.electronwill.night-config:core:${prop("night_config_version")}")
    "include"("com.electronwill.night-config:toml:${prop("night_config_version")}")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jspecify:jspecify:1.0.0")
}

tasks {
    processResources {
        // Three resource roots (this project + both shared trees) each ship an icon.png; loader/fabric's own copy is
        // the one referenced by fabric.mod.json and it comes first, so keep the first and drop the rest.
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        inputs.property("version", version)
        inputs.property("ae2_version", ae2Version)

        val replaceProperties = mapOf(
            "version" to version.toString(),
            "ae2_version" to ae2Version,
            "fabric_loader_version" to prop("fabric_loader_version"),
            "minecraft_version" to prop("minecraft_version"),
        )
        inputs.properties(replaceProperties)
        filesMatching("fabric.mod.json") {
            expand(replaceProperties)
        }
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

spotless {
    java {
        // Only this project's own overlay - the shared trees are formatted by the root project.
        target("src/**/java/**/*.java")

        endWithNewline()
        indentWithSpaces()
        removeUnusedImports()
        toggleOffOn()
        eclipse().configFile("$rootDir/codeformat/codeformat.xml")
        importOrderFile("$rootDir/codeformat/ae2wtlib.importorder")
    }
}
