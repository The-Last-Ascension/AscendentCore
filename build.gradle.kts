plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.46.1.3")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    shadowJar {
        archiveBaseName.set("AscendentCore")
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "com.thelastascension.ascendentCore.libs.hikari")
    }

    val pluginVersion = project.version.toString()
    val serverPluginsDir = layout.projectDirectory.dir("../../server/plugins")
    register<Copy>("deployToServer") {
        dependsOn(shadowJar)
        doFirst {
            serverPluginsDir.asFile.listFiles { _, name ->
                name.startsWith("AscendentCore-") && name.endsWith(".jar")
            }?.forEach { it.delete() }
        }
        from(shadowJar.flatMap { it.archiveFile })
        into(serverPluginsDir)
        rename { "AscendentCore-$pluginVersion.jar" }
    }

    build {
        dependsOn(shadowJar)
        finalizedBy("deployToServer")
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version.toString(), "description" to project.description.toString())
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
