plugins {
  alias(libs.plugins.jte)
  alias(libs.plugins.shadow)
  application
}

dependencies {
  implementation(project(":book-catalogue"))
  implementation(project(":open-library"))

  implementation(libs.bundles.exposed)
  implementation(libs.bundles.jackson)
  implementation(libs.bundles.javalin)
  implementation(libs.bundles.jte)
  implementation(libs.bundles.scrimage)
  implementation(libs.hoplite.core)
}

application {
  mainClass = "duckpond.buriedincode.BookshelfKt"
  applicationName = "Bookshelf"
}

jte {
  precompile()
  kotlinCompileArgs = arrayOf("-jvm-target", "17")
}

tasks.clean { doLast { delete("$projectDir/jte-classes") } }

tasks.jar {
  dependsOn(tasks.precompileJte)
  from(
    fileTree("jte-classes") {
      include("**/*.class")
      include("**/*.bin") // Only required if you use binary templates
    }
  )
  manifest.attributes["Main-Class"] = "duckpond.buriedincode.BookshelfKt"
}

tasks.shadowJar {
  dependsOn(tasks.precompileJte)
  from(
    fileTree("jte-classes") {
      include("**/*.class")
      include("**/*.bin") // Only required if you use binary templates
    }
  )
  manifest.attributes["Main-Class"] = "duckpond.buriedincode.BookshelfKt"
  mergeServiceFiles()
}
