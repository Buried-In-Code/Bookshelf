plugins {
  application
  alias(libs.plugins.jte)
  alias(libs.plugins.shadow)
}

dependencies {
  implementation(project(":openlibrary"))
  implementation(libs.bundles.exposed)
  implementation(libs.bundles.jackson)
  implementation(libs.bundles.javalin)
  implementation(libs.bundles.jte)
  implementation(libs.hoplite.core)
}

application {
  mainClass = "github.buriedincode.BookshelfKt"
  applicationName = "Bookshelf"
}

jte {
  precompile()
  kotlinCompileArgs = arrayOf("-jvm-target", "21")
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
  manifest.attributes["Main-Class"] = "github.buriedincode.BookshelfKt"
}

tasks.shadowJar {
  dependsOn(tasks.precompileJte)
  from(
    fileTree("jte-classes") {
      include("**/*.class")
      include("**/*.bin") // Only required if you use binary templates
    }
  )
  manifest.attributes["Main-Class"] = "github.buriedincode.BookshelfKt"
  mergeServiceFiles()
}
