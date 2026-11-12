plugins {
    id("net.neoforged.moddev")
}

val modId = providers.gradleProperty("mod_id").get()
val modVersion = providers.gradleProperty("mod_version").get()
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

neoForge {
    version = neoVersion

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            client()
            if (minecraftVersion == "26.1.2") {
                gameDirectory = rootProject.file("run")
            }
        }
        register("server") {
            server()
            programArgument("--nogui")
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
