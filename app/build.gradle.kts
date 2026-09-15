import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreFile = rootProject.file("key/key")
val keystorePropertiesFile = rootProject.file("key/keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val releaseSigningKeys = listOf("storePassword", "keyAlias", "keyPassword")
val releaseSigningProblems = buildList {
    if (!keystoreFile.isFile) add("Missing keystore: key/key")
    if (!keystorePropertiesFile.isFile) add("Missing credentials: key/keystore.properties")
    releaseSigningKeys.filter { keystoreProps.getProperty(it).isNullOrBlank() }
        .forEach { add("Missing signing property: $it") }
}
val hasReleaseSigning = releaseSigningProblems.isEmpty()

// Keep explicit release versions. Check Play Console before every upload.
val releaseVersionCode = 1
val releaseVersionName = "1.0"
val lastUploadedVersionCode = providers.gradleProperty("lastUploadedVersionCode").orNull
require(releaseVersionCode > 0) { "versionCode must be a positive integer." }
if (lastUploadedVersionCode != null) {
    val previous = lastUploadedVersionCode.toIntOrNull()
    require(previous != null && previous >= 0) {
        "lastUploadedVersionCode must be a non-negative integer from Play Console."
    }
    require(releaseVersionCode > previous) {
        "versionCode must be greater than lastUploadedVersionCode. Update app/build.gradle.kts."
    }
}

// A typed task keeps validation compatible with Gradle's configuration cache.
abstract class ValidateReleaseSigning : DefaultTask() {
    @get:Input
    abstract val problems: ListProperty<String>

    @TaskAction
    fun validate() {
        if (problems.get().isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured.\n" +
                    problems.get().joinToString("\n") +
                    "\nCreate key/keystore.properties using key/keystore.properties.example " +
                    "and supply the existing production keystore at key/key. " +
                    "Use assembleDebug for local development.",
            )
        }
    }
}

val validateProductionSigning = tasks.register<ValidateReleaseSigning>("validateProductionSigning") {
    group = "verification"
    description = "Reject release builds when production signing is unavailable."
    problems.set(releaseSigningProblems)
}

// Wire prerequisites, not command-line name matching, so aggregate/qualified tasks are safe too.
tasks.matching { it.name == "preReleaseBuild" || it.name == "validateSigningRelease" }.configureEach {
    dependsOn(validateProductionSigning)
}

android {
    namespace = "com.fitness.restlock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fitness.restlock"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["admobApplicationId"] = "ca-app-pub-3940256099942544~3347511713"
        resValue(
            "string",
            "admob_rewarded_finish_workout_ad_unit_id",
            "ca-app-pub-3940256099942544/5224354917",
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = keystoreFile
                storeType = "PKCS12"
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["admobApplicationId"] = "ca-app-pub-3940256099942544~3347511713"
            resValue(
                "string",
                "admob_rewarded_finish_workout_ad_unit_id",
                "ca-app-pub-3940256099942544/5224354917",
            )
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            manifestPlaceholders["admobApplicationId"] = "ca-app-pub-2584072112522734~9482454567"
            resValue(
                "string",
                "admob_rewarded_finish_workout_ad_unit_id",
                "ca-app-pub-2584072112522734/3207856102",
            )
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Missing credentials fail via validateProductionSigning; never use the debug key.
            signingConfig = signingConfigs.findByName("release")
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.android.gms:play-services-ads:24.7.0")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("com.google.truth:truth:1.4.4")
}
