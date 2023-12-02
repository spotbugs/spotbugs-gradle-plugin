plugins {
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
