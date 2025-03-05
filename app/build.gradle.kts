plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.mydiplom"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mydiplom"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-auth:22.1.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    testImplementation("junit:junit:4.13.2")
    implementation("com.google.firebase:firebase-storage:20.2.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")
    implementation ("com.google.android.material:material:1.11.0")
    implementation("com.github.CanHub:Android-Image-Cropper:4.3.2")

    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    // Добавляем зависимости для Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")  // Retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")  // Для конвертации JSON в объекты
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")  // Логирование запросов (опционально)

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.google.code.gson:gson:2.8.6")


    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
