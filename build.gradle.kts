plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin)
}

base {
    archivesName = property("archives_name") as String
}

version = property("mod_version") as String
group = property("maven_group") as String

repositories {
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.discord.ipc)
    include(libs.discord.ipc)
}

loom {
    accessWidenerPath = file("src/main/resources/volthack.accesswidener")

    mixin {
        defaultRefmapName = "volthack.refmap.json"
    }

    runs {
        named("client") {
            client()
            configName = "Client"
            ideConfigGenerated(true)
            vmArgs(
                "-Xmx2048M",
                "-Xms512M",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=20",
                "-XX:InitiatingHeapOccupancyPercent=45",
                "-XX:G1ReservePercent=15",
                "-XX:ReservedCodeCacheSize=256M",
                "-XX:MaxMetaspaceSize=256M",
                "-XX:CICompilerCount=4",
                "-XX:CompressedClassSpaceSize=128M",
                "-XX:MaxDirectMemorySize=512M"
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
}

val mcVersion: String = property("minecraft_version") as String
val loaderVersion: String = property("loader_version") as String

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "minecraft_version" to mcVersion,
        "loader_version" to loaderVersion
    )

    inputs.properties(props)

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}
kotlin {
    jvmToolchain(21)
}








