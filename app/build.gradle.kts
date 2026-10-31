plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.delivery.decision"; compileSdk = 35
 defaultConfig { applicationId = "com.delivery.decision"; minSdk = 29; targetSdk = 34; versionCode = 1; versionName = "0.1" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("com.google.mlkit:text-recognition-chinese:16.0.1") }