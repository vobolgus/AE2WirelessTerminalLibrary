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
    id("maven-publish")
}

val mavenGroup: String by project
val modID: String by project
val ae2wtlibCurrentMajor: String by project
// NOTE: the Fabric module resolves OUR AE2 fork, never upstream AE2 - it compiles the shared srcDirs
// (root + ae2wtlib_api) against `appliedenergistics2-fabric` from mavenLocal. Its pin therefore moves
// independently of the root project's upstream `ae2Version` (see gradle.properties / PORTING_NOTES 3.2).
val ae2FabricVersion: String by project

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
        // Team Reborn Energy - the Fabric energy API AE2 exposes its chargeable items over (seam #7, W4).
        // AE2's Fabric jar jar-in-jars it, so it is a RUNTIME transitive already; this repo only serves the
        // compileOnly stub (nested jars are not on a consumer's compile classpath).
        name = "TeamReborn"
        url = uri("https://maven.modmuss50.me/")
        content {
            includeGroup("teamreborn")
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

// ---------------------------------------------------------------------------
// W7 - the gametest companion mod's source set. Created BEFORE the `loom` block because that block
// references it eagerly (`mods { create("ae2wtlib_gametest") }` / `runs { create("gametest") }`).
// It sees everything `main` sees, plus `main`'s own output.
// ---------------------------------------------------------------------------
val gametestSourceSet: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets["main"].compileClasspath + sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].runtimeClasspath + sourceSets["main"].output
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
        // W7: the gametest suite is a COMPANION MOD with its own fabric.mod.json, so the test classes never
        // reach the release jar (`jar`/`remapJar` only see `main`). Same shape as the AE2 fork's harness.
        create("ae2wtlib_gametest") {
            sourceSet(gametestSourceSet)
        }
    }

    accessWidenerPath = file("src/main/resources/ae2wtlib.accesswidener")

    runs {
        configureEach {
            runDir = "run"
            // MIXIN_VERBOSE=1 ./gradlew :loader:fabric:runServer  ->  logs every mixin as it is applied. This is
            // the standing "are my mixin targets still valid" check after an AE2 rebase (PORTING_NOTES section 9.2);
            // `required: true` + `defaultRequire: 1` already turn a miss into a boot crash, but the log is what
            // tells you the expected COUNT is still what you think it is.
            if (System.getenv("MIXIN_VERBOSE") != null) {
                property("mixin.debug.verbose", "true")
            }
        }
        named("server") {
            programArgs("nogui")
        }
        // W7 - multiplayer repro harness (playbook Part 10, R5). Singleplayer and gametests NEVER serialize
        // packets over a socket, so a whole bug class (raw-id desync, codec mismatches) is invisible until a real
        // TCP join. Usage:
        //     QUICKPLAY_MP=localhost:25570 ./gradlew :loader:fabric:runClient
        // against a dedicated server carrying the IDENTICAL modset (see PORTING_NOTES §12.2). QUICKPLAY_SP is the
        // singleplayer twin, handy for the in-world checklist.
        named("client") {
            System.getenv("QUICKPLAY_MP")?.let { programArgs("--quickPlayMultiplayer", it) }
            System.getenv("QUICKPLAY_SP")?.let { programArgs("--quickPlaySingleplayer", it) }
        }
        // W7 gametest suite:  ./gradlew :loader:fabric:runGametest
        // fabric-api's MainMixin hijacks the dedicated-server main when `fabric-api.gametest` is set and boots a
        // vanilla GameTestServer (auto-agrees the EULA, no port bound, exits non-zero on failure).
        create("gametest") {
            server()
            name = "Game Test Server"
            runDir = "build/gametest"
            source(gametestSourceSet)
            property("fabric-api.gametest", "true")
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
            // W6 - recipe viewers. JEI and REI are IN (see the entrypoints in fabric.mod.json); only EMI is out:
            // upstream pins `dev.emi:emi-neoforge:1.1.22+1.21.1` and there is no 26.1 Fabric EMI artifact at all
            // (R7). The AE2 fork made the same call for its own EMI converter API.
            // ---------------------------------------------------------------
            exclude("de/mari_023/ae2wtlib/recipeviewer/AE2wtlibEmiPlugin.java")

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

// Dev-only runtime classpath: never published, never seen by consumers (loom's `runtimeOnly` would leak into
// the POM). Same construction as the AE2 fork uses for its item-list mods.
val localRuntimeOnly: Configuration = configurations.create("localRuntimeOnly")
configurations["runtimeClasspath"].extendsFrom(localRuntimeOnly)

dependencies {
    "minecraft"("com.mojang:minecraft:${prop("minecraft_version")}")
    // MC 26.1+ is unobfuscated: NO `mappings` line, and loom 1.17 dropped the `mod*` remapping
    // configurations - everything is a plain `implementation` (playbook Part 2).

    implementation("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")

    // Our AE2 Fabric fork, published to mavenLocal by `./gradlew :fabric:publishToMavenLocal` in the fork:
    //   ~/.m2/repository/org/appliedenergistics/appliedenergistics2-fabric/$ae2FabricVersion/
    // See PORTING_NOTES.md "AE2 dependency".
    implementation("org.appliedenergistics:appliedenergistics2-fabric:$ae2FabricVersion")
    // AE2's client classes (AEBaseScreen and friends) reference GuideME types in their signatures.
    compileOnly("org.appliedenergistics:guideme-fabric:${prop("guideme_fabric_version")}")

    // night-config backs the Fabric config store (the twin of NeoForge's ModConfigSpec). AE2's Fabric jar
    // already jar-in-jars the same coordinates; loader de-duplicates nested jars, and shipping our own keeps
    // this jar self-contained instead of depending on another mod's nested libraries.
    implementation("com.electronwill.night-config:toml:${prop("night_config_version")}")
    "include"("com.electronwill.night-config:core:${prop("night_config_version")}")
    "include"("com.electronwill.night-config:toml:${prop("night_config_version")}")

    // W4 seam #7 - Team Reborn Energy, the Fabric energy API AE2 exposes chargeable items over.
    // compileOnly and NOT `include`d on purpose: AE2's Fabric jar already jar-in-jars teamreborn:energy and
    // declares it in its runtimeElements, so it is present at runtime (the runServer/runClient mod list shows
    // `team_reborn_energy 5.0.0`). It is absent from AE2's apiElements, which is why the compile stub is needed.
    compileOnly("teamreborn:energy:${prop("tr_energy_version")}")

    // ---------------------------------------------------------------------------------------------------
    // W6 - recipe viewers. Both are compileOnly: the plugins are discovered through fabric.mod.json
    // entrypoints and simply never load if the viewer is absent.
    // ---------------------------------------------------------------------------------------------------
    // JEI: the `-fabric-api` artifact carries the Fabric-only classes and pulls `-common-api` transitively.
    // ⚠ Fabric does NOT scan the @JeiPlugin annotation (that is NeoForge's discovery); the annotation itself
    // lives in the common API, so the shared JEIPlugin compiles unchanged and the `jei_mod_plugin` entrypoint
    // does the discovering.
    compileOnly("mezz.jei:jei-${prop("jeiMinecraftVersion")}-fabric-api:${prop("jeiVersion")}")

    // REI: the `-neoforge` API artifacts are used deliberately - since REI 26.1.x both flavours are mojmap and
    // carry an identical API, and this is the pin our AE2 fork proved on 26.1.819. transitive=false because
    // their poms drag in cloth-config-neoforge & co. that we neither need nor want resolved here; architectury
    // (REI's fluid entry type) and cloth basic-math are pinned explicitly instead. Nothing here ever ships:
    // at runtime the real REI provides the classes.
    compileOnly("me.shedaniel:RoughlyEnoughItems-api-neoforge:${prop("reiVersion")}") { isTransitive = false }
    compileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-neoforge:${prop("reiVersion")}") {
        isTransitive = false
    }
    compileOnly("dev.architectury:architectury-neoforge:${prop("architecturyVersion")}") { isTransitive = false }
    compileOnly("me.shedaniel.cloth:basic-math:${prop("cloth_basic_math_version")}") { isTransitive = false }

    // Dev-runtime item-list mod, so the two entrypoints above can actually be exercised by runClient.
    // `localRuntimeOnly` keeps it off every published/consumed classpath (same shape as the AE2 fork).
    // Select with -PruntimeItemlistMod=rei|jei|none (the root project's existing property).
    when (providers.gradleProperty("runtimeItemlistMod").getOrElse("none")) {
        "rei" -> {
            localRuntimeOnly("me.shedaniel:RoughlyEnoughItems-fabric:${prop("reiVersion")}")
            localRuntimeOnly("dev.architectury:architectury-fabric:${prop("architecturyVersion")}")
        }

        "jei" -> localRuntimeOnly("mezz.jei:jei-${prop("jeiMinecraftVersion")}-fabric:${prop("jeiVersion")}")
        // "emi": no 26.1 Fabric EMI artifact exists (R7) - the dev client just runs without an item-list mod.
    }

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jspecify:jspecify:1.0.0")
}

tasks {
    processResources {
        // Three resource roots (this project + both shared trees) each ship an icon.png; loader/fabric's own copy is
        // the one referenced by fabric.mod.json and it comes first, so keep the first and drop the rest.
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        inputs.property("version", version)
        inputs.property("ae2_version", ae2FabricVersion)

        val replaceProperties = mapOf(
            "version" to version.toString(),
            "ae2_version" to ae2FabricVersion,
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.appliedenergistics"
            artifactId = "ae2wtlib-fabric"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
