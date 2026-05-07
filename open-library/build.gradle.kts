plugins {
  alias(libs.plugins.kotlinx.serialization)
  `java-library`
}

dependencies {
  implementation(libs.kotlinx.serialization)

  testImplementation(libs.junit.jupiter)

  testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
  useJUnitPlatform()
  testLogging { events("passed", "skipped", "failed") }
}
