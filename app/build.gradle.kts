import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")

}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.example.madproject"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.madproject"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties["GEMINI_API_KEY"] ?: ""}\"")
        manifestPlaceholders["MAPS_API_KEY"] = localProperties["MAPS_API_KEY"] as? String ?: ""
    }

    buildFeatures {
        buildConfig = true
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    androidTestImplementation("com.google.firebase:firebase-auth")
    androidTestImplementation("com.google.firebase:firebase-firestore")
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
    implementation("com.google.guava:guava:31.1-android")
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    // OkHttp for Gemini API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // WorkManager for background jobs
    implementation("androidx.work:work-runtime:2.9.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage")

}