/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    mapOf("group" to "teamcity-aws-sns-trigger-plugin", "version" to versionNumber, "artifact" to "aws-sns-trigger")
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
        maven { url = URI("https://download.jetbrains.com/teamcity-repository") }
        maven { url = URI("https://repository.jetbrains.com/all") }
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
