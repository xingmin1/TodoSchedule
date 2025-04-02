buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.gradle)
        classpath(libs.ktlint.gradle)
        classpath(libs.git.hooks.gradle.plugin)
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.library") version "8.9.1" apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    id("com.github.jakemarsden.git-hooks") version "0.0.2"
}

// 配置 git-hooks 插件
gitHooks {
    setHooks(mapOf("pre-commit" to "scripts/pre-commit"))
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
