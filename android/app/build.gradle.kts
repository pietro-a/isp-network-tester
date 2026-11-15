import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

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

// Keep release credentials outside the repository. If the file is absent,
// assembleRelease still works but produces an unsigned APK.
val signingPropertiesFile = File(
    System.getProperty("user.home"),
    ".config/isp-network-tester/signing.properties",
)
val signingProperties = Properties()
if (signingPropertiesFile.isFile) {
    signingPropertiesFile.inputStream().use(signingProperties::load)
}

val releaseSigningConfigured = signingPropertiesFile.isFile
val releaseStoreFile = signingProperties.getProperty("storeFile")?.let { configuredPath ->
    val expandedPath = if (configuredPath == "~") {
        System.getProperty("user.home")
    } else {
        configuredPath.replaceFirst("~/", "${System.getProperty("user.home")}/")
    }
    File(expandedPath).let { if (it.isAbsolute) it else rootProject.file(expandedPath) }
}
val releaseStorePassword = signingProperties.getProperty("storePassword")
val releaseKeyAlias = signingProperties.getProperty("keyAlias")
val releaseKeyPassword = signingProperties.getProperty("keyPassword")

if (releaseSigningConfigured) {
    val missingProperties = buildList {
        if (releaseStoreFile == null) add("storeFile")
        if (releaseStorePassword.isNullOrBlank()) add("storePassword")
        if (releaseKeyAlias.isNullOrBlank()) add("keyAlias")
        if (releaseKeyPassword.isNullOrBlank()) add("keyPassword")
    }
    if (missingProperties.isNotEmpty()) {
        throw GradleException(
            "Release signing properties are incomplete: ${missingProperties.joinToString()}"
        )
    }
    if (releaseStoreFile?.isFile != true) {
        throw GradleException("Release keystore does not exist: $releaseStoreFile")
    }
}

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

    signingConfigs {
        create("release") {
            if ( releaseSigningConfigured) {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
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

// Keep the normal Gradle build task, but give its release APK a useful name.
tasks.named("assembleRelease") {
    doLast {
        val releaseOutputDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val generatedApk = listOf("app-release.apk", "app-release-unsigned.apk")
            .asSequence()
            .map { File(releaseOutputDir, it) }
            .firstOrNull(File::isFile)
            ?: throw GradleException("Could not find the generated release APK in $releaseOutputDir")

        val safeVersion = appVersion.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val namedApk = File(releaseOutputDir, "isp-network-tester-$safeVersion.apk")
        if (generatedApk != namedApk) {
            if (namedApk.exists() && !namedApk.delete()) {
                throw GradleException("Could not replace existing APK: $namedApk")
            }
            if (!generatedApk.renameTo(namedApk)) {
                throw GradleException("Could not rename $generatedApk to $namedApk")
            }
        }
        logger.lifecycle("Release APK: $namedApk")
    }
}
