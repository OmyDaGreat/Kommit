plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.yaml)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest("2.1.0")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("kommit")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

application {
    mainClass = "xyz.malefic.cli.CommitGeneratorKt"
}
