import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localProperty(key: String): String? {
    return localProperties.getProperty(key)?.takeIf { it.isNotBlank() }
}

val hasReleaseSigning = listOf(
    "KEYSTORE_PATH",
    "KEYSTORE_PASSWORD",
    "KEY_ALIAS",
    "KEY_PASSWORD",
).all { localProperty(it) != null }

android {
    namespace = "com.jj.zds"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jj.zds"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(localProperty("KEYSTORE_PATH")!!)
                storePassword = localProperty("KEYSTORE_PASSWORD")!!
                keyAlias = localProperty("KEY_ALIAS")!!
                keyPassword = localProperty("KEY_PASSWORD")!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

val zdsApkOutputDir = localProperty("APK_OUTPUT_DIR")
    ?: "D:/apk/宗益"

fun registerZdsApkCopyTask(
    buildType: String,
    assembleTaskName: String,
) {
    val taskName = "copyZds${buildType.replaceFirstChar { it.uppercase() }}Apk"
    val copyTask = tasks.register<Copy>(taskName) {
        group = "build"
        description = "Copy zds $buildType APK to output directory."
        dependsOn(assembleTaskName)

        val versionName = android.defaultConfig.versionName ?: "unknown"
        from(layout.buildDirectory.dir("outputs/apk/$buildType"))
        include("*.apk")
        into(file(zdsApkOutputDir).resolve(buildType))
        rename { "dzs-v$versionName-$buildType.apk" }

        doLast {
            println("zds $buildType APK output: ${file(zdsApkOutputDir).resolve(buildType).absolutePath}")
        }
    }

    afterEvaluate {
        tasks.named(assembleTaskName) {
            finalizedBy(copyTask)
        }
    }
}

registerZdsApkCopyTask("debug", "assembleDebug")
registerZdsApkCopyTask("release", "assembleRelease")

dependencies {
    implementation(project(":jiang-core"))
    implementation(project(":jiang-arch"))
    implementation(project(":jiang-ui"))
    implementation(project(":jiang-network"))
    implementation(project(":jiang-storage"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.android.material)
    debugImplementation(libs.chucker.library)
    releaseImplementation(libs.chucker.library.no.op)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
