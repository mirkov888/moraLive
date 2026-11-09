plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "id.mora.live"
    compileSdk = 35
    defaultConfig {
        applicationId = "id.mora.live"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "0.4.3"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("MORA_KEYSTORE_FILE")
            val storePass = System.getenv("MORA_KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("MORA_KEY_ALIAS")
            val keyPass = System.getenv("MORA_KEY_PASSWORD")
            if (!storePath.isNullOrBlank() && !storePass.isNullOrBlank() &&
                !keyAliasValue.isNullOrBlank() && !keyPass.isNullOrBlank()
            ) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = keyAliasValue
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.webkit:webkit:1.12.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
