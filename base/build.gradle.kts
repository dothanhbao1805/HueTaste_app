@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.base"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        targetSdk = 36
    }

    testOptions {
        targetSdk = 36
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    implementation(libs.recycler.view)
    implementation(libs.rx.pref)
    implementation(libs.rx.kotlin)
    implementation(libs.timber)
    implementation(libs.paging)
    implementation(libs.bundles.sdp.ssp.libs)
    implementation(libs.bundles.uber.autodispose.libs)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.lifecycle.process)

    // Window
    implementation(libs.window)
}