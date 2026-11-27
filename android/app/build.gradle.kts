import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.meetingscheduler"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.example.meetingscheduler"
        minSdk = 24
        targetSdk = 35
        versionCode = 117
        versionName = "0.45.54"
        resValue("string", "app_name", "JW Meeting Manager")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_FILE")
            val storePass = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val alias = System.getenv("ANDROID_KEY_ALIAS")
            val keyPass = System.getenv("ANDROID_KEY_PASSWORD")
            if (!keystorePath.isNullOrBlank() &&
                !storePass.isNullOrBlank() &&
                !alias.isNullOrBlank() &&
                !keyPass.isNullOrBlank()
            ) {
                storeFile = file(keystorePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            resValue("string", "app_name", "JW Meeting Manager Preview")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

abstract class BundleAppChangelog : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val changelog: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyChangelog() {
        changelog.get().asFile.copyTo(
            outputDirectory.file("app-changelog.md").get().asFile,
            overwrite = true
        )
    }
}

val standaloneChangelog = rootProject.layout.projectDirectory.file("CHANGELOG.md")
val checkoutChangelog = rootProject.layout.projectDirectory.file("../CHANGELOG.md")
val bundleAppChangelog = tasks.register<BundleAppChangelog>("bundleAppChangelog") {
    changelog.set(if (standaloneChangelog.asFile.isFile) standaloneChangelog else checkoutChangelog)
    outputDirectory.set(layout.buildDirectory.dir("generated/app-changelog/assets"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(bundleAppChangelog, BundleAppChangelog::outputDirectory)
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-messaging")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.adamglin:phosphor-icon:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
