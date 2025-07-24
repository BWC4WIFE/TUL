import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
        )
    )
}
android {
    namespace = "com.bwc.tul"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bwc.tul"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.9"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        dataBinding = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }
}


        /** dependencies {
            // Core Android
            implementation("androidx.core:core-ktx:1.12.0")
            implementation("androidx.appcompat:appcompat:1.6.1")
            implementation("com.google.android.material:material:1.11.0")
            implementation("androidx.constraintlayout:constraintlayout:2.1.4")

            // Lifecycle
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
            implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
            implementation("androidx.activity:activity-ktx:1.8.2")
            implementation("androidx.fragment:fragment-ktx:1.6.2")

            // Compose (using BOM)

            implementation("androidx.compose:compose-bom:2024.05.00")
            implementation("androidx.compose.ui:ui")
            implementation("androidx.compose.ui:ui-graphics")
            implementation("androidx.compose.ui:ui-tooling-preview")
            implementation("androidx.compose.material3:material3")
            implementation("androidx.activity:activity-compose:1.8.2")
            debugImplementation("androidx.compose.ui:ui-tooling")
            debugImplementation("androidx.compose.ui:ui-test-manifest")

            // Networking
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
            implementation("com.google.code.gson:gson:2.10.1")

            // Room
            implementation("androidx.room:room-runtime:2.6.1")
            implementation("androidx.room:room-ktx:2.6.1")
            kapt("androidx.room:room-compiler:2.6.1")

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

            // Testing
            testImplementation("junit:junit:4.13.2")
            androidTestImplementation("androidx.test.ext:junit:1.1.5")
            androidTestImplementation("androidx.compose:compose-bom:2024.05.00")
            androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
            androidTestImplementation("androidx.compose.ui:ui-test-junit4")
            val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
            implementation(composeBom)

            // Then declare your Compose dependencies without versions
            implementation("androidx.compose.ui:ui")
            implementation("androidx.compose.ui:ui-tooling-preview")
            implementation("androidx.compose.material3:material3")

            debugImplementation("androidx.compose.ui:ui-tooling")
            debugImplementation("androidx.compose.ui:ui-test-manifest")
        }
        */
        dependencies{
val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
implementation(composeBom)
androidTestImplementation(composeBom)

// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")

// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.8.2")

debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

// Networking
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("com.google.code.gson:gson:2.10.1")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Testing
testImplementation("junit:junit:4.13.2")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
