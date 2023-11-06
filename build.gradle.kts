buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter() // needed for: rxfingerprint, rximagepicker
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter() // needed for: rxfingerprint, rximagepicker
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
