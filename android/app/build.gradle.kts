import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

/* Design-token codegen. Reads design-tokens/tokens.json (shared with the web)
 * and writes android/app/src/main/java/com/remitos/app/ui/theme/Tokens.kt.
 * The generated file is committed; CI runs `:app:checkTokens` to verify it is
 * in sync. Do NOT hand-edit Tokens.kt — change tokens.json then run
 * `./gradlew :app:genTokens`. */
val designTokensFile: File = rootProject.file("../design-tokens/tokens.json")
val generatedTokensFile: File = file(
    "src/main/java/com/remitos/app/ui/theme/Tokens.kt",
)

fun renderTokensKt(json: Map<String, Any?>): String {
    @Suppress("UNCHECKED_CAST")
    val color = json["color"] as Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val brand = color["brand"] as Map<String, String>
    @Suppress("UNCHECKED_CAST")
    val neutral = color["neutral"] as Map<String, String>
    @Suppress("UNCHECKED_CAST")
    val semantic = color["semantic"] as Map<String, String>
    @Suppress("UNCHECKED_CAST")
    val surface = color["surface"] as Map<String, String>
    @Suppress("UNCHECKED_CAST")
    val button = color["button"] as Map<String, String>
    @Suppress("UNCHECKED_CAST")
    val radius = json["radius"] as Map<String, Number>
    @Suppress("UNCHECKED_CAST")
    val spacing = json["spacing"] as Map<String, Number>
    @Suppress("UNCHECKED_CAST")
    val type = json["type"] as Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val scale = type["scale"] as Map<String, Map<String, Number>>

    fun hex(s: String): String {
        val raw = s.removePrefix("#").uppercase()
        val withAlpha = if (raw.length == 6) "FF$raw" else raw
        return "0x$withAlpha"
    }

    val sb = StringBuilder()
    sb.appendLine("// AUTO-GENERATED FROM design-tokens/tokens.json — DO NOT EDIT.")
    sb.appendLine("// Run `./gradlew :app:genTokens` after editing tokens.json.")
    sb.appendLine("@file:Suppress(\"unused\", \"MayBeConstant\")")
    sb.appendLine()
    sb.appendLine("package com.remitos.app.ui.theme")
    sb.appendLine()
    sb.appendLine("import androidx.compose.ui.graphics.Color")
    sb.appendLine("import androidx.compose.ui.text.font.FontWeight")
    sb.appendLine("import androidx.compose.ui.unit.dp")
    sb.appendLine("import androidx.compose.ui.unit.sp")
    sb.appendLine("import androidx.compose.ui.unit.TextUnit")
    sb.appendLine("import androidx.compose.ui.unit.Dp")
    sb.appendLine()
    sb.appendLine("object Tokens {")
    sb.appendLine("    object Color {")
    sb.appendLine("        // Brand")
    sb.appendLine("        val brandPrimary = Color(${hex(brand.getValue("primary"))})")
    sb.appendLine("        val brandPrimaryHover = Color(${hex(brand.getValue("primaryHover"))})")
    sb.appendLine("        val brandPrimaryPressed = Color(${hex(brand.getValue("primaryPressed"))})")
    sb.appendLine("        val brandPrimarySubtle = Color(${hex(brand.getValue("primarySubtle"))})")
    sb.appendLine("        val brandPrimarySurface = Color(${hex(brand.getValue("primarySurface"))})")
    sb.appendLine("        val brandRed = Color(${hex(brand.getValue("red"))})")
    sb.appendLine("        val brandRedHover = Color(${hex(brand.getValue("redHover"))})")
    sb.appendLine("        val brandRedSubtle = Color(${hex(brand.getValue("redSubtle"))})")
    sb.appendLine()
    sb.appendLine("        // Neutrals")
    for ((k, v) in neutral) {
        sb.appendLine("        val ${k} = Color(${hex(v)})")
    }
    sb.appendLine()
    sb.appendLine("        // Semantic")
    sb.appendLine("        val success = Color(${hex(semantic.getValue("success"))})")
    sb.appendLine("        val successSubtle = Color(${hex(semantic.getValue("successSubtle"))})")
    sb.appendLine("        val successOn = Color(${hex(semantic.getValue("successOn"))})")
    sb.appendLine("        val warning = Color(${hex(semantic.getValue("warning"))})")
    sb.appendLine("        val warningSubtle = Color(${hex(semantic.getValue("warningSubtle"))})")
    sb.appendLine("        val warningOn = Color(${hex(semantic.getValue("warningOn"))})")
    sb.appendLine("        val error = Color(${hex(semantic.getValue("error"))})")
    sb.appendLine("        val errorSubtle = Color(${hex(semantic.getValue("errorSubtle"))})")
    sb.appendLine("        val errorOn = Color(${hex(semantic.getValue("errorOn"))})")
    sb.appendLine("        val info = Color(${hex(semantic.getValue("info"))})")
    sb.appendLine("        val infoSubtle = Color(${hex(semantic.getValue("infoSubtle"))})")
    sb.appendLine("        val infoOn = Color(${hex(semantic.getValue("infoOn"))})")
    sb.appendLine()
    sb.appendLine("        // Surface")
    sb.appendLine("        val surfaceBackground = Color(${hex(surface.getValue("background"))})")
    sb.appendLine("        val surfaceMuted = Color(${hex(surface.getValue("muted"))})")
    sb.appendLine("        val surfaceBorder = Color(${hex(surface.getValue("border"))})")
    sb.appendLine("        val surfaceText = Color(${hex(surface.getValue("text"))})")
    sb.appendLine("        val surfaceTextMuted = Color(${hex(surface.getValue("textMuted"))})")
    sb.appendLine()
    sb.appendLine("        // Buttons")
    sb.appendLine("        val buttonDisabledBackground = Color(${hex(button.getValue("disabledBackground"))})")
    sb.appendLine("        val buttonDisabledContent = Color(${hex(button.getValue("disabledContent"))})")
    sb.appendLine("    }")
    sb.appendLine()
    sb.appendLine("    object Radius {")
    for ((k, v) in radius) {
        sb.appendLine("        val ${k}: Dp = ${v.toInt()}.dp")
    }
    sb.appendLine("    }")
    sb.appendLine()
    sb.appendLine("    object Spacing {")
    for ((k, v) in spacing) {
        sb.appendLine("        val ${k}: Dp = ${v.toInt()}.dp")
    }
    sb.appendLine("    }")
    sb.appendLine()
    sb.appendLine("    object Type {")
    sb.appendLine("        const val family: String = \"${type["family"]}\"")
    sb.appendLine("        data class Style(val size: TextUnit, val lineHeight: TextUnit, val weight: FontWeight, val letterSpacing: TextUnit)")
    for ((name, props) in scale) {
        val size = props.getValue("size")
        val lineHeight = props.getValue("lineHeight")
        val weight = props.getValue("weight").toInt()
        val letterSpacing = props.getValue("letterSpacing")
        sb.appendLine(
            "        val ${name} = Style(${size}.sp, ${lineHeight}.sp, FontWeight(${weight}), ${letterSpacing}.sp)",
        )
    }
    sb.appendLine("    }")
    sb.appendLine("}")
    return sb.toString()
}

fun generateTokensKtContents(): String {
    val parsed = JsonSlurper().parse(designTokensFile) as Map<String, Any?>
    return renderTokensKt(parsed)
}

val genTokens by tasks.registering {
    group = "build"
    description = "Regenerates ui/theme/Tokens.kt from design-tokens/tokens.json."
    inputs.file(designTokensFile)
    outputs.file(generatedTokensFile)
    doLast {
        val content = generateTokensKtContents()
        generatedTokensFile.parentFile.mkdirs()
        generatedTokensFile.writeText(content)
        println("genTokens: wrote ${generatedTokensFile.relativeTo(rootProject.rootDir)}")
    }
}

/* checkTokens deliberately does NOT depend on genTokens; it compares the
 * committed Tokens.kt against what tokens.json would produce. CI runs this
 * before any compilation. */
tasks.register("checkTokens") {
    group = "verification"
    description = "Fails if ui/theme/Tokens.kt is out of sync with design-tokens/tokens.json."
    inputs.file(designTokensFile)
    mustRunAfter(genTokens)
    doLast {
        val expected = generateTokensKtContents()
        val actual = if (generatedTokensFile.exists()) generatedTokensFile.readText() else ""
        if (expected != actual) {
            throw GradleException(
                "Tokens.kt is out of sync with design-tokens/tokens.json. " +
                    "Run `./gradlew :app:genTokens` and commit the result.",
            )
        }
        println("checkTokens: Tokens.kt in sync with tokens.json")
    }
}

tasks.named("preBuild") {
    dependsOn(genTokens)
}

/** Regenerate with `./gradlew :app:generateThirdPartyLicensesTxt` when Gradle dependencies change. */
tasks.register("generateThirdPartyLicensesTxt") {
    group = "build"
    description =
        "Writes src/main/assets/THIRD_PARTY_LICENSES.txt from :app releaseRuntimeClasspath (coordinates only)."
    val out = layout.projectDirectory.file("src/main/assets/THIRD_PARTY_LICENSES.txt")
    outputs.file(out)

    doLast {
        val cfg = configurations.named("releaseRuntimeClasspath").get()
        val ids =
            cfg.incoming.resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }
                .distinctBy { "${it.group}:${it.name}:${it.version}" }
                .sortedWith(compareBy({ it.group }, { it.name }, { it.version }))

        val text = buildString {
            appendLine("En Punto — Android app")
            appendLine("Third-party libraries (releaseRuntimeClasspath)")
            appendLine()
            for (id in ids) {
                val (g, n, v) =
                    if (id.group == "remitos" && id.name == "opencv" && id.version == "unspecified") {
                        // Matches publishing {} in opencv-sdk/.../sdk/build.gradle
                        Triple("org.opencv", "opencv", "4.12.0")
                    } else {
                        Triple(id.group, id.name, id.version)
                    }
                appendLine("$g:$n:$v")
            }
        }
        out.asFile.parentFile.mkdirs()
        out.asFile.writeText(text)
        println("generateThirdPartyLicensesTxt: wrote ${ids.size} entries to ${out.asFile}")
    }
}

/** Debug API base: `-PBACKEND_BASE_URL=...` > android/local.properties > default emulator host. */
val remitosLocalProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

fun debugBackendBaseUrl(): String {
    val fromCli = (project.findProperty("BACKEND_BASE_URL") as String?)?.trim()?.takeIf { it.isNotEmpty() }
    val fromFile = remitosLocalProperties.getProperty("BACKEND_BASE_URL")?.trim()?.takeIf { it.isNotEmpty() }
    val base = (fromCli ?: fromFile ?: "http://10.0.2.2:8080").trimEnd('/')
    return "$base/"
}

android {
    namespace = "com.remitos.app"
    compileSdk = 35

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    defaultConfig {
        applicationId = "com.remitos.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 21
        versionName = "0.2.0-alpha04"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_RELEASE_KEYSTORE")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            val backend = debugBackendBaseUrl()
            println("app:debug BACKEND_BASE_URL = $backend")
            val escaped = backend.replace("\\", "\\\\").replace("\"", "\\\"")
            buildConfigField("String", "BACKEND_BASE_URL", "\"$escaped\"")
        }
        release {
            buildConfigField(
                "String",
                "BACKEND_BASE_URL",
                "\"https://remitos-api-865349418409.southamerica-east1.run.app/\"",
            )
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val useReleaseKeystore = !System.getenv("ANDROID_RELEASE_KEYSTORE").isNullOrBlank()
            signingConfig = if (useReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui:1.6.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.2")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.2")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.2")
    implementation("androidx.compose.animation:animation:1.6.2")
    implementation("com.google.android.material:material:1.11.0")
    
    // Coil for Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Security
    // 1.1.x stable not yet on Maven for MasterKey + AES256_SIV; keep alpha until stable matches AuthManager API.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation(project(":opencv"))

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // CameraX for document photo capture
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

    // ML Kit text recognition for OCR
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Google Sign-In and Drive API
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.http-client:google-http-client-gson:1.44.1")
    implementation("com.google.api-client:google-api-client-android:2.4.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20240809-2.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling:1.6.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")

    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
