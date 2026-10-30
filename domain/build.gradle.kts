import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.datetime)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidUnitTest.dependencies { implementation(libs.junit) }
        androidMain.dependencies {
        }
    }
}

android {
    namespace = "pe.edu.hzg.tesoreria.domain"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}
dependencies { coreLibraryDesugaring(libs.desugar.jdk.libs) }
