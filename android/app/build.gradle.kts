import java.net.URL
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Bundle a pinned Google Sans Flex build so export typography is the same offline on every device.
val googleSansRevision = "a6452c14f7567eb588816c2db5898c522e607e4b"
val googleSansFont = layout.projectDirectory.file("src/main/assets/fonts/GoogleSansFlex.ttf").asFile
val googleSansLicense = layout.projectDirectory.file("src/main/assets/fonts/OFL.txt").asFile
val prepareGoogleSansExportFont by tasks.registering {
    outputs.files(googleSansFont, googleSansLicense)
    outputs.upToDateWhen { false }
    doLast {
        fun ensurePinnedAsset(file: java.io.File, url: String, expectedSha256: String) {
            fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
                .digest(bytes).joinToString("") { "%02x".format(it) }
            if (file.isFile && digest(file.readBytes()) == expectedSha256) return
            val connection = URL(url).openConnection().apply {
                connectTimeout = 20_000
                readTimeout = 30_000
            }
            val bytes = connection.getInputStream().use { it.readBytes() }
            check(digest(bytes) == expectedSha256) { "SHA-256 check failed for ${file.name}" }
            file.parentFile.mkdirs()
            file.writeBytes(bytes)
        }
        ensurePinnedAsset(
            googleSansFont,
            "https://raw.githubusercontent.com/googlefonts/googlesans-flex/$googleSansRevision/documentation/demo-fonts/GoogleSansFlex%5BGRAD%2CROND%2Copsz%2Cslnt%2Cwdth%2Cwght%5D-144B3.ttf",
            "0c9cd7e90daabec83b94a73d90a1d2f659f7b671203ba80cc860ad651224d4f7"
        )
        ensurePinnedAsset(
            googleSansLicense,
            "https://raw.githubusercontent.com/googlefonts/googlesans-flex/$googleSansRevision/OFL.txt",
            "831b6e041705873c3e6efb18c4c96202bc41c8be42a456d3d26c47abf14816d0"
        )
    }
}
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(prepareGoogleSansExportFont)
}

android {
    namespace = "com.example.meetingscheduler"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.example.meetingscheduler"
        minSdk = 24
        targetSdk = 35
        versionCode = 66
        versionName = "0.45.3"
        resValue("string", "app_name", "JW Meeting Scheduler")
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
            resValue("string", "app_name", "JW Meeting Scheduler Preview")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}


