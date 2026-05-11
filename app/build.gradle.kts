import java.util.Properties
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Read signing properties outside android {} to avoid DSL name shadowing.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().also { props ->
    if (keystorePropsFile.exists()) props.load(keystorePropsFile.inputStream())
}

android {
    namespace = "org.hearth.circuits"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.hearth.circuits"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps["keystorePath"] as String)
                storePassword = keystoreProps["keystorePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (keystorePropsFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
        }
        create("foss") {
            dimension = "distribution"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        buildConfig = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        abortOnError = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.webkit)
    implementation(libs.material)
    implementation(libs.androidx.core.splashscreen)

    lintChecks(project(":lint-checks"))

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Reproducible builds: strip timestamps and sort entries in every archive task.
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.register("assertNoGms") {
    group = "verification"
    description = "Fails if GMS or Firebase dependency is declared in any configuration"
    // Capture at configuration time so doLast needs no project access.
    val forbidden = listOf("com.google.android.gms", "com.google.firebase")
    val hits: List<String> = project.configurations
        .flatMap { cfg ->
            runCatching {
                cfg.dependencies
                    .filterIsInstance<ExternalDependency>()
                    .mapNotNull { it.group }
                    .filter { g -> forbidden.any { g.startsWith(it) } }
                    .map { g -> "${cfg.name}: $g" }
            }.getOrDefault(emptyList())
        }
    inputs.property("gmsHits", hits)
    doLast {
        @Suppress("UNCHECKED_CAST")
        val gmsHits = inputs.properties["gmsHits"] as List<String>
        require(gmsHits.isEmpty()) {
            "Forbidden GMS/Firebase dependency found:\n${gmsHits.joinToString("\n")}"
        }
    }
}

tasks.named("check") {
    dependsOn("assertNoGms")
}
