import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val apiBaseUrl = providers.gradleProperty("API_BASE_URL")
    .orElse("https://sistema.cetprohzgj.edu.pe")
    .get()

val appVersionCode = providers.gradleProperty("APP_VERSION_CODE")
    .orElse("1")
    .map(String::toInt)
    .get()

val appVersionName = providers.gradleProperty("APP_VERSION_NAME")
    .orElse("1.0.0")
    .get()

val releaseSigningFile = rootProject.file("keystore.properties")
val releaseSigningProperties = Properties()
val releaseSigningKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")

if (releaseSigningFile.isFile) {
    releaseSigningFile.inputStream().use(releaseSigningProperties::load)
    val missingKeys = releaseSigningKeys.filter { releaseSigningProperties.getProperty(it).isNullOrBlank() }
    require(missingKeys.isEmpty()) {
        "keystore.properties esta incompleto. Faltan: ${missingKeys.joinToString()}"
    }
}

val hasReleaseSigning = releaseSigningFile.isFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pe.edu.hzg.tesoreria"
    compileSdk = 34

    defaultConfig {
        applicationId = "pe.edu.hzg.tesoreria"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    sourceSets {
        androidMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(project(":presentation"))
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.android)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.android)
        }
        androidUnitTest.dependencies { implementation(libs.junit) }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.android.material)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.6")
    androidTestImplementation(libs.koin.android.compose)
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}
