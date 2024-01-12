

plugins {
    id("com.github.rodm.teamcity-server") version "1.4.1"
}

val teamcityVersion = ext.get("teamcityVersion")!! as String

teamcity {
    version = teamcityVersion
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
        archiveBaseName.set(projectIds["artifact"])
    }
    compileKotlin {
        java {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}