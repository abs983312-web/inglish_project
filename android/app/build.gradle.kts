plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "uz.inglish.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "uz.inglish.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // Default works in the emulator (10.0.2.2 = host machine).
            // For a real phone pass the host address, e.g.:
            //   ./gradlew assembleDebug -PapiBaseUrl=http://192.168.1.20:3000/
            // or with USB port forwarding:
            //   ./gradlew assembleDebug -PapiBaseUrl=http://127.0.0.1:3000/
            val rawBaseUrl = (project.findProperty("apiBaseUrl") as String?)
                ?: "http://10.0.2.2:3000/"
            val apiBaseUrl = if (rawBaseUrl.endsWith("/")) rawBaseUrl else "$rawBaseUrl/"
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            // Debug builds are sideloaded and cannot reach Play, so the paywall
            // uses the server's mock purchase tokens instead of Google Play.
            val billingMock = ((project.findProperty("billingMock") as String?) ?: "true").toBoolean()
            buildConfigField("Boolean", "BILLING_MOCK", billingMock.toString())
        }

        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.inglish.uz/\"")
            buildConfigField("Boolean", "BILLING_MOCK", "false")
            // TODO: enable R8 once the release build and serialization rules are verified.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.billing.ktx)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register<JavaExec>("unitTests") {
    group = "verification"
    description = "Runs pure unit tests directly using JUnitCore"
    dependsOn("compileDebugUnitTestKotlin")
    classpath = files(
        layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"),
        layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
        configurations.getByName("debugUnitTestRuntimeClasspath"),
    )
    mainClass.set("org.junit.runner.JUnitCore")
    args(
        "uz.inglish.app.core.TextUtilsTest",
        "uz.inglish.app.feature.vocabulary.FsrsCardTest",
        "uz.inglish.app.core.CommonUtilsTest",
    )
}
