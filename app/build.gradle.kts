plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.todoschedule"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.todoschedule"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }

        // 针对中文环境优化
        androidResources.localeFilters += listOf("zh-rCN", "en")
    }

    // 禁用图片压缩以加速构建
    androidResources {
        // 不对以下格式执行压缩处理
        noCompress += listOf("png", "jpg", "jpeg", "webp", "gif")
    }

    buildTypes {
        release {
            isMinifyEnabled = true // 启用代码压缩
            isShrinkResources = true // 启用资源压缩
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            // "-Xopt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 核心库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // SplashScreen API
    implementation(libs.androidx.core.splashscreen)

    // Moshi JSON Parsing
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
    
    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.constraintlayout.compose)

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt 依赖注入
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    // Room 数据库
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Retrofit 网络请求
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation(libs.logging.interceptor)

    //拼音库
    implementation("com.belerweb:pinyin4j:2.5.1")

    //jsoup库
    implementation("org.jsoup:jsoup:1.14.3")

    // 日期时间处理
    implementation(libs.kotlinx.datetime)

    // Kotlin 序列化
    implementation(libs.kotlinx.serialization.json)

    // HLC - Hybrid Logical Clock
    implementation("com.github.charlietap:hlc:1.0.1")

    // Synk CRDT 数据同步库
    implementation("com.github.charlietap.synk:delightful-metastore:0.38")
    implementation("com.github.charlietap.synk:synk:0.38")

    // SQLDelight - 用于数据库访问
    implementation("app.cash.sqldelight:android-driver:2.0.1")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")

    // WorkManager - 用于后台任务
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Room
    androidTestImplementation(libs.androidx.room.testing)

    // AndroidX Test Core & JUnit
    androidTestImplementation(libs.core.ktx)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.test.runner)

    // Coroutines Test
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Truth Assertions (提供更流畅的断言)
    androidTestImplementation(libs.truth) // 或更高版本

    // Hilt Testing
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)

    // Android Architecture Components testing - for InstantTaskExecutorRule
    androidTestImplementation(libs.androidx.core.testing)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Password Hashing
    implementation(libs.jbcrypt)
}