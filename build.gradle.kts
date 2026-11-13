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

dependencies {
    compileOnly("maven.modrinth:jade:26.1.12+neoforge")
    compileOnly("maven.modrinth:jei:60HrmNPh")
    compileOnly("me.shedaniel:RoughlyEnoughItems-neoforge:26.1.819")
    compileOnly("dev.architectury:architectury-neoforge:20.1.17")
    compileOnly("top.theillusivec4.curios:curios-neoforge:15.0.0+26.1.2")
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
            if (minecraftVersion == "26.1.2") {
                gameDirectory = rootProject.file("run")
            }
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
    val metadata = mapOf(
        "mod_id" to modId,
        "mod_version" to modVersion,
        "minecraft_version" to minecraftVersion,
        "neo_version" to neoVersion
    )
    inputs.properties(metadata)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(metadata)
    }
}

tasks.jar {
    from(rootProject.file("LICENSE"))
}
