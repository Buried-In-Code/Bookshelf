plugins {
  alias(libs.plugins.jte)
  alias(libs.plugins.shadow)
  application
  id("com.github.node-gradle.node")
}

dependencies {
  implementation(project(":openlibrary"))
  implementation(libs.bundles.exposed)
  implementation(libs.bundles.jackson)
  implementation(libs.bundles.javalin)
  implementation(libs.bundles.jte)
  implementation(libs.hoplite.core)
  implementation("org.springframework.security:spring-security-crypto:6.5.4")

  runtimeOnly("org.bouncycastle:bcprov-jdk18on:1.81")
}

application {
  mainClass = "github.buriedincode.bookshelf.ServerKt"
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
  manifest.attributes["Main-Class"] = "github.buriedincode.bookshelf.ServerKt"
}

tasks.shadowJar {
  dependsOn(tasks.precompileJte)
  from(
    fileTree("jte-classes") {
      include("**/*.class")
      include("**/*.bin") // Only required if you use binary templates
    }
  )
  manifest.attributes["Main-Class"] = "github.buriedincode.bookshelf.ServerKt"
  mergeServiceFiles()
}

node {
  download = false
  version = "22.19.0"
}

val npmInstall by tasks.getting(com.github.gradle.node.npm.task.NpmTask::class)

val compileSass by
  tasks.registering(com.github.gradle.node.npm.task.NpxTask::class) {
    dependsOn(npmInstall)
    command = "sass"
    args =
      listOf("src/main/resources/scss/main.scss:src/main/resources/static/css/bulma-custom.css", "--style=compressed")
    inputs.dir("src/main/resources/scss")
    outputs.file("src/main/resources/static/css/bulma-custom.css")
  }

// tasks.processResources { dependsOn(compileSass) }
