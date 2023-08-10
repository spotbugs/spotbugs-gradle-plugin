plugins {
    id("com.diffplug.spotless")
}

// TODO introduce KDoc

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
