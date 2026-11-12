plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        // Warnings only until the codebase has been formatted once with `./gradlew ktlintFormat`.
        ignoreFailures.set(true)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    source.setFrom(
        files(
            "app/src/main/java",
            "core-parser/src/main/kotlin",
            "vault-access/src/main/java",
            "scheduler/src/main/java",
            "designsystem/src/main/java",
        ),
    )
    // Report-only for now, see CONTRIBUTING.md.
    ignoreFailures = true
}

// detekt 1.23.7 is compiled against Kotlin 2.0.10.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.10")
    }
}
