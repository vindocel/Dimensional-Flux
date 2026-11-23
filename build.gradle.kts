plugins {
    id("net.neoforged.moddev")
}

val modId = providers.gradleProperty("mod_id").get()
// gradle.properties holds the base version of the open update; a release build takes its full version from the tag
// (-Prelease_version=0.1.2-beta.1, see scripts/validate-release.sh), so alphas and betas need no version commit.
val modVersion = providers.gradleProperty("release_version").orElse(providers.gradleProperty("mod_version")).get()
val minecraftVersion = sc.current.version
val neoVersion = property("neo_version") as String
val javaVersion = (property("java_version") as String).toInt()

group = modId
version = "$modVersion+$minecraftVersion"
base.archivesName = "dimensional-flux"
java.toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.latvian.dev/releases") }
        filter { includeGroup("dev.latvian.mods") }
    }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
    // optional REI integration (compile-only API; nothing is bundled)
    exclusiveContent {
        forRepository { maven("https://maven.shedaniel.me/") }
        filter {
            includeGroup("me.shedaniel")
            includeGroup("me.shedaniel.cloth")
            includeGroup("dev.architectury")
        }
    }
    exclusiveContent {
        forRepository { maven("https://maven.theillusivec4.top/") }
        filter { includeGroup("top.theillusivec4.curios") }
    }
}

// Optional integrations are pinned per Minecraft version in versions/<minecraft>/gradle.properties.
// A target without a build of an integration leaves its property out, and its compat classes are not compiled.
fun optionalVersion(name: String) = findProperty(name) as String?

dependencies {
    val kubejs = optionalVersion("kubejs_version")
    val rhino = optionalVersion("rhino_version")
    if (kubejs != null && rhino != null) {
        compileOnly("dev.latvian.mods:kubejs-neoforge:$kubejs") { isTransitive = false }
        compileOnly("dev.latvian.mods:rhino:$rhino") { isTransitive = false }
    }
    optionalVersion("jade_version")?.let { compileOnly("maven.modrinth:jade:$it") }
    optionalVersion("jei_version")?.let { compileOnly("maven.modrinth:jei:$it") }
    optionalVersion("rei_version")?.let { compileOnly("me.shedaniel:RoughlyEnoughItems-neoforge:$it") }
    optionalVersion("architectury_version")?.let { compileOnly("dev.architectury:architectury-neoforge:$it") }
    optionalVersion("curios_version")?.let { compileOnly("top.theillusivec4.curios:curios-neoforge:$it") }
}

if (optionalVersion("kubejs_version") == null) {
    sourceSets.main {
        java.exclude("dimensionalflux/compat/kubejs/**")
        resources.exclude("kubejs.plugins.txt")
    }
}

// The GameTests: their own source set and test mod (dimensionalflux_gametest), loaded only by the gameTestServer run,
// so they never reach the released jar or the other runs.
// Stonecutter adds src/gametest of the repository root to this source set, as it does for src/main.
val gametest = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations.named(gametest.compileOnlyConfigurationName) { extendsFrom(configurations.compileOnly.get()) }

neoForge {
    version = neoVersion
    addModdingDependenciesTo(gametest)

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
        register("dimensionalflux_gametest") {
            sourceSet(gametest)
        }
    }

    runs {
        register("client") {
            client()
            loadedMods = setOf(mods.getByName(modId))
            // Version-specific instances keep mods, settings and saves independent.
            gameDirectory = rootProject.file("run/$minecraftVersion")
        }
        register("server") {
            server()
            loadedMods = setOf(mods.getByName(modId))
            programArgument("--nogui")
        }
        // ./gradlew :<minecraft>:runGameTestServer runs every GameTest and stops; CI and releases look for
        // "required tests passed" in its output.
        register("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gametest
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            programArguments.addAll("--tests", "$modId:*")
        }
        configureEach {
            val runRoot = providers.gradleProperty("test_run_root").orNull
            if (runRoot != null) {
                gameDirectory = rootProject.file(runRoot).resolve("$minecraftVersion/$name")
            }
        }
    }
}

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}

tasks.processResources {
    // versions/<minecraft>/src/main/resources holds the files whose format differs on that version (loot tables on
    // 26.3, for example); each one replaces the shared file at the same path.
    val versionResources = project.file("src/main/resources")
    if (versionResources.isDirectory) {
        val replaced = fileTree(versionResources).files
            .map { it.relativeTo(versionResources).invariantSeparatorsPath }.toSet()
        exclude { it.relativePath.pathString in replaced && !it.file.startsWith(versionResources) }
    }
    // World generation data moved in Minecraft 26.3: configured features (worldgen/configured_feature, with a
    // "config" object) became features (worldgen/feature). Both forms are kept and each version ships its own.
    // Shaders: from 26.3 the game links vertex and fragment shaders separately, so its shaders declare their inputs
    // and outputs by location; those versions use the shaders in shaders/core/separate.
    if (sc.current.parsed >= "26.3") {
        exclude("data/*/worldgen/configured_feature/**")
        exclude("assets/*/shaders/core/steelframe_*")
    } else {
        exclude("data/*/worldgen/feature/**")
        exclude("assets/*/shaders/core/separate/**")
    }
    val metadata = mapOf(
        "mod_id" to modId,
        "mod_version" to modVersion,
        "minecraft_version" to minecraftVersion,
        "neo_version" to neoVersion,
        // the mod list reads the banner and icon from bannerFile and iconFile from 26.3; earlier versions from logoFile
        "mod_images" to if (sc.current.parsed >= "26.3") "bannerFile=\"logo.png\"\niconFile=\"icon.png\"" else "logoFile=\"logo.png\""
    )
    inputs.properties(metadata)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(metadata)
    }
}

tasks.jar {
    from(rootProject.file("LICENSE"))
}
