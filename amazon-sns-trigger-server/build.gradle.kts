import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    id("io.github.rodm.teamcity-server") version "1.5.6"
}

val teamcityVersion = ext.get("teamcityVersion")!! as String

teamcity {
    version = teamcityVersion
    allowSnapshotVersions = true

    server {
        descriptor = project.file("teamcity-plugin.xml")
        tokens = mapOf("Plugin_Version" to project.version)

        files {
            into("kotlin-dsl") {
                from("../kotlin-dsl")
            }
        }
    }
}

dependencies {
    provided("org.jetbrains.teamcity.internal:server:$teamcityVersion")
    provided("org.jetbrains.teamcity:server-openapi:$teamcityVersion")
    provided("org.jetbrains.teamcity:server-core:$teamcityVersion")

    testImplementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("io.mockk:mockk:1.12.7")
}

val projectIds = ext.get("projectIds")!! as Map<String, String>

tasks {
    serverPlugin {
        archiveVersion.convention(null as String?)
        archiveVersion.set(null as String?)
        archiveBaseName.convention(projectIds["artifact"])
        archiveFileName.set(projectIds["artifact"] + ".zip")
    }
}
