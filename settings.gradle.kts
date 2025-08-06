plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "Bookshelf"

include("openlibrary")

include("app")
