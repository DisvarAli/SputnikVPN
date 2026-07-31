plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val devAbi = providers.gradleProperty("devAbi").orNull
val isReleaseBuild = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

val libv2rayMarker = layout.projectDirectory.file("libs/libv2ray.aar")

val ensureLibv2ray = tasks.register("ensureLibv2ray") {
    val marker = libv2rayMarker
    outputs.file(marker)
    doLast {
        val f = marker.asFile
        if (f.exists() && f.length() > 50_000_000L) return@doLast
        val rootDir = rootProject.layout.projectDirectory.asFile
        val script = rootProject.layout.projectDirectory.file("scripts/download-libs.ps1").asFile
        val shScript = rootProject.layout.projectDirectory.file("scripts/download-libs.sh").asFile
        val code = if (System.getProperty("os.name").lowercase().contains("windows")) {
            ProcessBuilder(
                "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", script.absolutePath
            ).directory(rootDir).inheritIO().start().waitFor()
        } else {
            ProcessBuilder("bash", shScript.absolutePath)
                .directory(rootDir).inheritIO().start().waitFor()
        }
        if (code != 0 || !f.exists() || f.length() < 50_000_000L) {
            error("libv2ray download failed — run scripts/download-libs.ps1 or download-libs.sh")
        }
    }
}

tasks.named("preBuild").configure { dependsOn(ensureLibv2ray) }

android {
    namespace = "com.my.vpn"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.my.vpn"
        minSdk = 26
        targetSdk = 36
        versionCode = 21
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.clear()
            abiFilters += when {
                devAbi != null -> listOf(devAbi)
                isReleaseBuild -> listOf("arm64-v8a", "armeabi-v7a")
                else -> listOf("arm64-v8a")
            }
        }
    }

    flavorDimensions += "channel"
    productFlavors {
        create("stable") {
            dimension = "channel"
        }
        create("dev") {
            dimension = "channel"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "assets/geosite.dat"
            excludes += "assets/geoip.dat"
            excludes += "assets/geoip-only-cn-private.dat"
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
