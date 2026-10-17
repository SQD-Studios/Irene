plugins {
    id("java")
    id("maven-publish")

    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "chamosmpRepoReleases"
        url = uri("https://maven.chamosmp.net/releases")
    }
    maven("https://eldonexus.de/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://nexus.scarsz.me/content/groups/public/")
    maven("https://repo.essentialsx.net/releases/")
    mavenCentral()
}

dependencies {
    compileOnly(libs.paper)

    implementation(libs.sqdlib.paper)
    implementation(libs.bstats.bukkit)

    // StrokkCommands
    compileOnly(libs.strokk.paper)
    annotationProcessor(libs.strokk.paper.proccesor)

    // Plugin Dependencies
    compileOnly(libs.luckperms)
    compileOnly(libs.placeholderapi)

    // Discord Integration
    compileOnly(libs.discordsrv)
    compileOnly(libs.essx)
    compileOnly(libs.essx.discord)


    // Plugin Messaging
    compileOnly(libs.lettuce)
    compileOnly(libs.jnats)
    compileOnly(libs.rabbitmq)
}

group = "net.chamosmp.irene"
version = "OB2-1.0.0"
description = "Irene, is a chat plugin, which extends to chat moderation, and linking chats between servers"
java.sourceCompatibility = JavaVersion.VERSION_25

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks {
    shadowJar {
        configurations = project.configurations.runtimeClasspath.map { setOf(it) }

        relocate("net.chamosmp.sqdlib", "net.chamosmp.irene.libs.sqdlib")
        relocate("org.bstats", "net.chamosmp.irene.libs.bstats")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    withType<Javadoc> {
        options.encoding = "UTF-8"
    }

    processResources {
        val props = mapOf("version" to project.version, "description" to project.description)
        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    runServer {
        downloadPlugins {
            modrinth("lKEzGugV", "2.12.3")
            modrinth("Vebnzrzj", "v5.5.53-bukkit")
            //modrinth("UmLGoGij", "1.30.5") // DiscordSRV
        }

        minecraftVersion("26.2")
    }
}