plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

tasks.register("asbzds") {
    group = "build"
    description = "Assemble debug and release APKs for zds, then copy them to the APK output directory."
    dependsOn(
        ":app:copyZdsDebugApk",
        ":app:copyZdsReleaseApk",
    )
}
