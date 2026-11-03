plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.metroup"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.metroup"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.core:core-ktx:1.16.0")
    testImplementation("junit:junit:4.13.2")
}

// OneDrive can keep old binary-results directories locked on Windows.
// Keep XML/HTML report paths stable; give each test run its own binary output.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-binary/$name/${System.currentTimeMillis()}"))
}
