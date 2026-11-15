import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun readGitMetadata(script: String): String =
    providers.exec {
        isIgnoreExitValue = true
        workingDir(rootProject.projectDir.parentFile)
        commandLine("bash", "scripts/$script")
    }.standardOutput.asText.get().trim()

fun buildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val gitVersion = readGitMetadata("version.sh")
val gitRevision = readGitMetadata("revision.sh")
val appVersion = gitVersion.ifBlank {
    if (gitRevision.isBlank()) "v0.0.0" else "v0.0.0-$gitRevision"
}
val appRevision = gitRevision


android {
    namespace = "space.antonov.ispnetworktester"
    compileSdk = 36

    defaultConfig {
        applicationId = "space.antonov.ispnetworktester"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = appVersion
        buildConfigField("String", "REVISION", buildConfigString(appRevision))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.icmp4a)
    implementation("androidx.compose.material:material-icons-core")
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
