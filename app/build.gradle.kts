plugins {
    alias(libs.plugins.android.application) version "8.2.2"
    alias(libs.plugins.kotlin.android) version "1.9.22"
    }

android {
    namespace = "com.bwc.translator2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bwc.translator2"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "BWC_TRANS2_${variant.buildType.name}_${variant.versionName}.apk"
                output.outputFileName = outputFileName
            }
    }

/*    *//**//*applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .filter {
                val names = it.name.split("-")
                it.name.lowercase().contains(names[0], true) && it.name.lowercase().contains(names[1], true)
            }
            .forEach { output ->
                val outputFileName = "BWC_TRANS2_${variant.buildType.name}_${variant.versionName}.apk"
                output.outputFileName = outputFileName
            }
    }*//**/

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // --- Set the APK/AAB file name for RELEASE builds ---
            // Example 1: Basic name with version
            // archiveFileName.set("Translator2-v${defaultConfig.versionName}.apk")

            // Example 2: More detailed name including version, build type, and build time
            // We'll use defaultConfig.versionName, defaultConfig.versionCode, and buildType.name
            // You can also get project.version if you define it at the project level
            //("Translator2-v${defaultConfig.versionName}(${defaultConfig.versionCode})-release.apk")

            // If you're building an AAB (Android App Bundle) instead of an APK:
            // archiveFileName.set("Translator2-v${defaultConfig.versionName}(${defaultConfig.versionCode})-release.aab")


        }
    }

    buildFeatures {
        dataBinding {
            enable = true
        }
        viewBinding {
            enable = true
        }
        compose = true

    }


    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
   }

dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.google.gson)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.logging.interceptor)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}