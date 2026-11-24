plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cl.rarce.calculadorapro"
    compileSdk = 36

    defaultConfig {
        applicationId = "cl.rarce.calculadorapro"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "0.4.2"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val signingPath = System.getenv("CALCPRO_KEYSTORE_PATH")
    val signingStorePassword = System.getenv("CALCPRO_STORE_PASSWORD")
    val signingAlias = System.getenv("CALCPRO_KEY_ALIAS")
    val signingKeyPassword = System.getenv("CALCPRO_KEY_PASSWORD")
    val canSignRelease = listOf(signingPath, signingStorePassword, signingAlias, signingKeyPassword).all { !it.isNullOrBlank() }
    if (canSignRelease) {
        signingConfigs {
            create("calcProRelease") {
                storeFile = file(signingPath!!)
                storePassword = signingStorePassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
            }
        }
    }
    buildTypes {
        release {
            if (canSignRelease) signingConfig = signingConfigs.getByName("calcProRelease")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
