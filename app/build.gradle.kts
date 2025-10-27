plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.dentalflow.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dentalflow.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    // Flavors por entorno
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            // Si existen en local.properties, los usa; si no, defaults seguros
            val baseUrl = (project.findProperty("DEV_BASE_URL") as String?)
                ?: "http://10.0.2.2:3000/api"
            val apiKey = (project.findProperty("DEV_API_KEY") as String?)
                ?: "demo-secret-change-me"

            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            buildConfigField("String", "API_KEY", "\"$apiKey\"")
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("staging") {
            dimension = "env"
            val baseUrl = (project.findProperty("STAGING_BASE_URL") as String?)
                ?: "https://staging.example.com/api"
            val apiKey = (project.findProperty("STAGING_API_KEY") as String?)
                ?: "replace-me"
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            buildConfigField("String", "API_KEY", "\"$apiKey\"")
            applicationIdSuffix = ".stg"
            versionNameSuffix = "-stg"
        }
        create("prod") {
            dimension = "env"
            val baseUrl = (project.findProperty("PROD_BASE_URL") as String?)
                ?: "https://api.example.com/api"
            val apiKey = (project.findProperty("PROD_API_KEY") as String?)
                ?: "replace-me"
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            buildConfigField("String", "API_KEY", "\"$apiKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        languageVersion = "1.9"
        apiVersion = "1.9"
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

tasks.register("printKotlinTargets") {
    doLast {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().forEach {
            println("${it.name} -> jvmTarget=${it.kotlinOptions.jvmTarget}")
        }
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.media3.common.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
