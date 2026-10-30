import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.multiplatform.lifecycle.viewmodel)
            implementation(libs.multiplatform.lifecycle.runtime)
            implementation(libs.multiplatform.navigation)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidUnitTest.dependencies { implementation(libs.junit) }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
            implementation(libs.androidx.core.ktx)
        }
    }
}

android {
    namespace = "pe.edu.hzg.tesoreria.presentation"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}
dependencies { coreLibraryDesugaring(libs.desugar.jdk.libs) }
