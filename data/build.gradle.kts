import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidUnitTest.dependencies { implementation(libs.junit) }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.koin.android)
        }
        androidUnitTest.dependencies { implementation(libs.sqldelight.sqlite.driver) }
    }
}

android {
    namespace = "pe.edu.hzg.tesoreria.data"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}
dependencies { coreLibraryDesugaring(libs.desugar.jdk.libs) }

sqldelight {
    databases {
        create("AppDatabase") { packageName.set("pe.edu.hzg.tesoreria.db") }
    }
}
