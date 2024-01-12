

import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
}

val correctVersion = project.hasProperty("versionNumber") && "\\d+\\.\\d+\\.\\d+.*".toRegex()
    .matches(property("versionNumber") as CharSequence)
val versionNumber =
    if (correctVersion) property("versionNumber") else "SNAPSHOT-" + SimpleDateFormat("yyyyMMddHHmmss").format(Date())
val projectIds =
    mapOf(
        "group" to "teamcity-amazon-sns-trigger-plugin",
        "version" to versionNumber,
        "artifact" to "amazon-sns-trigger"
    )
val teamcityVersion = if (project.hasProperty("teamcityVersion")) property("teamcityVersion") else "2022.08"

allprojects {
    group = projectIds["group"]!!
    version = projectIds["version"]!!

    ext {
        set("correctVersion", correctVersion)
        set("versionNumber", versionNumber)
        set("projectIds", projectIds)
        set("teamcityVersion", teamcityVersion)
    }

    repositories {
        mavenLocal()
        maven { url = URI("https://download.jetbrains.com/teamcity-repository") }
        mavenCentral()
        google()
    }
}

subprojects {
    apply {
        plugin("kotlin")
    }

    tasks {
        test {
            useJUnitPlatform()
        }
        jar {
            archiveVersion.convention(null as String?)
            archiveVersion.set(null as String?)
        }
    }
}