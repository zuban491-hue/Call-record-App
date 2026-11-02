plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.callcapture.app"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.callcapture.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.version"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.cc.setup.lifecycle.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}

// This is a publication-data guard, not a policy-approval or security certificate.
val validateLegalPublication by tasks.registering {
    inputs.file("src/main/res/values/legal_config.xml")
    doLast {
        val text = file("src/main/res/values/legal_config.xml").readText()
        fun field(name: String): String = Regex("""<string\s+name="$name"[^>]*>(.*?)</string>""",
            RegexOption.DOT_MATCHES_ALL).find(text)?.groupValues?.get(1)?.trim().orEmpty()
        val required = listOf("legal_developer_name", "legal_contact_email", "legal_privacy_url", "legal_terms_url")
        required.forEach { name ->
            val value = field(name)
            check(value.isNotBlank() && !value.contains("{{")) { "Supply verified $name before a release build." }
        }
        check(Regex("""[^\s@]+@[^\s@]+\.[^\s@]+""").matches(field("legal_contact_email"))) { "Provide a valid developer contact email." }
        listOf("legal_privacy_url", "legal_terms_url").forEach { name ->
            val url = java.net.URI(field(name))
            check(url.scheme == "https" && !url.host.isNullOrBlank() && url.userInfo == null && !url.path.orEmpty().endsWith(".pdf", true)) {
                "$name must be a public HTTPS web page, not a PDF."
            }
            check(url.host !in listOf("localhost", "example.com", "example.org", "example.net") && !url.host.endsWith(".example")) {
                "Replace the placeholder/local URL in $name."
            }
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateLegalPublication) }
