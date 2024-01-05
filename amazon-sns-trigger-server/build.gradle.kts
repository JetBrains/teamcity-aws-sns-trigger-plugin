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

plugins {
    id("com.github.rodm.teamcity-server") version "1.5.2"
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
