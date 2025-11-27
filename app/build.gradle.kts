plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	kotlin("kapt")
//	alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
	alias(libs.plugins.secrets.gradle.plugin)
}

android {
	namespace = "com.fusion5.dyipqrxml"
	compileSdk = 36

	defaultConfig {
		applicationId = "com.fusion5.dyipqrxml"
		minSdk = 24
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
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
	secrets {
		propertiesFileName = "secrets.properties"
		defaultPropertiesFileName = "local.defaults.properties"
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.navigation.fragment.ktx)
	implementation(libs.androidx.navigation.ui.ktx)
	implementation(libs.androidx.navigation.runtime.ktx)
	implementation(libs.androidx.camera.core)
	implementation(libs.androidx.camera.camera2)
	implementation(libs.androidx.camera.lifecycle)
	implementation(libs.androidx.camera.view)
	implementation(libs.androidx.lifecycle.viewmodel.ktx)

	implementation(libs.kotlinx.coroutines.core)
	implementation(libs.kotlinx.coroutines.android)

	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	kapt(libs.androidx.room.compiler)

	implementation(libs.androidx.datastore.preferences)

	// ML Kit Barcode Scanning for QR codes
	implementation("com.google.mlkit:barcode-scanning:17.3.0")

	// Google Maps and Location Services
	implementation(libs.play.services.maps)
	implementation("com.google.android.gms:play-services-location:21.3.0")
    // Maps Utils for GeoJSON
    implementation("com.google.maps.android:android-maps-utils:3.8.2")

	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}