plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

android {
    namespace = "ru.orangesoftware.financisto"

    compileSdk = 34

    defaultConfig {
        applicationId = "ru.orangesoftware.financisto"
        minSdk = 19
        targetSdk = 28
        versionCode = 122
        versionName = "1.8.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        javaCompileOptions {
            annotationProcessorOptions {
                argument("androidManifestFile", "$projectDir/src/main/AndroidManifest.xml")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        animationsDisabled = true
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    tasks.withType<Test> {
        testLogging {
            events("failed", "passed", "skipped")
            setExceptionFormat("full")
            showStandardStreams = true
        }
        reports.junitXml.required.set(true)
        reports.html.required.set(true)
        outputs.upToDateWhen { false }
    }

    sourceSets["test"].resources.srcDirs("src/test/resources")

    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES.txt")
        resources.excludes.add("META-INF/LICENSE")
        resources.excludes.add("META-INF/LICENSE.txt")
        resources.excludes.add("META-INF/NOTICE.txt")
        resources.excludes.add("META-INF/NOTICE")
        resources.excludes.add("META-INF/LICENSE")
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/notice.txt")
        resources.excludes.add("META-INF/license.txt")
        resources.excludes.add("META-INF/dependencies.txt")
        resources.excludes.add("META-INF/LGPL2.1")
    }
    lint {
        // If set to true, turns off analysis progress reporting by lint.
        quiet = true
        // If set to true (default), stops the build if errors are found.
        abortOnError = false
        // If set to true, lint only reports errors.
        ignoreWarnings = false
        // If set to true, lint also checks all dependencies as part of its analysis.
        // Recommended for projects consisting of an app with library dependencies.
        checkDependencies = true

        checkReleaseBuilds = false
    }
}

val AAVersion = "4.8.0"

dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.gms:play-services-drive:17.0.0")
    implementation("com.google.android.gms:play-services-plus:17.0.0")

    kapt("org.androidannotations:androidannotations:$AAVersion")
    implementation("org.androidannotations:androidannotations-api:$AAVersion")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.sf.trove4j:trove4j:3.0.3")

    implementation("com.dropbox.core:dropbox-core-sdk:5.4.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.6.0")
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("com.wdullaer:materialdatetimepicker:4.2.3")
    implementation("commons-io:commons-io:2.15.0")
    implementation("com.github.bumptech.glide:glide:4.10.0")

    // Rx libs
    implementation("com.github.akarnokd:rxjava3-bridge:3.0.2")
    implementation("com.mtramin:rxfingerprint:2.2.1")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("org.reactivestreams:reactive-streams:1.0.4")
    implementation("com.mlsdev.rximagepicker:library:2.1.5")

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1") // can't be used because of the current target sdk
    testImplementation("org.hamcrest:hamcrest:2.2")
}

kapt {
    keepJavacAnnotationProcessors = true
    showProcessorStats = true
    useBuildCache = true
    correctErrorTypes = true
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
//        vendor = JvmVendorSpec.ADOPTIUM
    }
}
kotlin {
    jvmToolchain(17)
//    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(8))
}
