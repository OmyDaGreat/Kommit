plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "xyz.malefic.cli.main"
            }
        }
    }

    macosArm64 {
        binaries {
            executable {
                entryPoint = "xyz.malefic.cli.main"
            }
        }
    }

    mingwX64 {
        binaries {
            executable {
                entryPoint = "xyz.malefic.cli.main"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotter)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kaml)
                implementation(libs.kotlinx.io)
                implementation(libs.kommand)
                implementation(libs.okio)
            }
        }
    }

    jvmToolchain(27)
}

val kommitVersion = providers.gradleProperty("kommitVersion").orElse("dev")

val generateVersion =
    tasks.register("generateVersion") {
        description = "Generates a Kotlin file containing the current version of the application."
        val version = kommitVersion
        val outDir = layout.buildDirectory.dir("generated/version/kotlin")
        inputs.property("version", version)
        outputs.dir(outDir)
        doLast {
            val file = outDir.get().file("xyz/malefic/cli/Version.kt").asFile
            file.parentFile.mkdirs()
            file.writeText(
                """
                package xyz.malefic.cli

                internal const val VERSION = "${version.get()}"
                """.trimIndent() + "\n",
            )
        }
    }

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateVersion)
}
