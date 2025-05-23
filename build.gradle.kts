import java.io.FileInputStream
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

initializeWorkspace()

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
val teamcityVersion = anyParam("teamcityVersion") ?: "2025.03"
val localRepo = anyParamPath("TC_LOCAL_REPO")

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
    if (localRepo != null) {
      maven(url = "file:///${localRepo}")
    }
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

fun anyParamPath(vararg names: String): Path? {
  val param = anyParam(*names)
  if (param == null || param.isEmpty())
    return null
  return if (Paths.get(param).isAbsolute) {
    Paths.get(param)
  } else {
    getRootDir().toPath().resolve(param)
  }
}

fun anyParam(vararg names: String): String? {
  var param: String? = ""
  try {
    for (name in names) {
      param = if (project.hasProperty(name)) {
        project.property(name).toString()
      } else {
        System.getProperty(name) ?: System.getenv(name)
      }
      if (param != null)
        break
    }
    if (param == null || param.isEmpty())
      param = null
  } finally {
    println("AnyParam: ${names.joinToString(separator = ",")} -> $param")
  }
  return param
}


fun initializeWorkspace() {
  if (System.getProperty("idea.active") != null) {
    println("Attempt to configure workspace in IDEA")
    val coreVersionProperties = project.projectDir.toPath().parent.parent.resolve(".version.properties")
    if (coreVersionProperties.toFile().exists()) {
      val p = Properties().also {
        it.load(FileInputStream(coreVersionProperties.toFile()))
      }
      p.forEach { (k, v) ->
        System.setProperty(k.toString(), v.toString())
      }
    }
  }
}
