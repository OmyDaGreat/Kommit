plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

kotlin {
    // Configure native targets
    linuxX64 {
        binaries {
            executable {
                entryPoint = "xyz.malefic.cli.main"
            }
        }
    }
    
    macosX64 {
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
            }
        }
    }
}
