plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // if project build.gradle.kts have alias google.services use alias:
     alias(libs.plugins.google.services)
    //id("com.google.gms.google-services")
}

configurations.all {
    resolutionStrategy {
        force("androidx.test:runner:1.5.2")
        force("androidx.test:rules:1.5.0")
        force("androidx.test.ext:junit:1.1.5")
        force("androidx.test.espresso:espresso-core:3.5.1")
        force("androidx.test.espresso:espresso-contrib:3.5.1")
        force("androidx.test.espresso:espresso-intents:3.5.1")
    }
}

android {
    namespace = "com.cs250.kratos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cs250.kratos"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Kotlin -> JVM 17
    kotlin {
        //  JDK 17
        jvmToolchain(17)
    }

    buildFeatures {
        viewBinding = true
        dataBinding = false
    }
}

dependencies {
    // Firebase BOM + services
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    testImplementation(libs.junit)
    testImplementation("androidx.arch.core:core-testing:2.2.0") // For InstantTaskExecutorRule
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1") // For mocking

    androidTestImplementation("org.mockito:mockito-android:5.11.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0") // For UI Automator

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
