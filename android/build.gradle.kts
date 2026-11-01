// Standalone build: the root build applies the desktop plugins to every subproject, so this one is not included in
// it. Run it with `./gradlew -p android assembleDebug`, with ANDROID_HOME pointing at an Android SDK.

plugins {
    id("com.android.application") version "9.2.1"
}

// AGP's bundled Kotlin stops at JVM target 24 while tt needs Java 25 syntax, so javac is raised after configuration.
val declaredJavaLevel = JavaVersion.VERSION_24
val sourceJavaLevel = JavaVersion.VERSION_25

val repoRoot = rootDir.parentFile
val portDir = layout.buildDirectory.dir("port/java")
val buildInfoDir = layout.buildDirectory.dir("port/buildinfo")

// Desktop-only classes whose Android counterparts live in android/src/main/java.
val desktopOnly = listOf(
    "module-info.java",
    "com/oddlabs/net/DeserializationFilter.java",
    "com/oddlabs/tt/Main.java",
    "com/oddlabs/tt/bugreport/**",
    "com/oddlabs/tt/window/LWJGL3Window.java",
    "com/oddlabs/tt/window/DesktopWindowSystem.java",
    "com/oddlabs/tt/input/LWJGL3InputProvider.java",
    "com/oddlabs/tt/input/scripted/**",
)

// java.io.IO is Java 25 and missing from android.jar; the sources are copied, so the originals stay as they are.
val portSources by tasks.registering(Sync::class) {
    into(portDir)
    from(File(repoRoot, "common/src/main/java")) { exclude(desktopOnly) }
    from(File(repoRoot, "tt/src/main/java")) { exclude(desktopOnly) }
    filter { line -> line.replace(Regex("""\bIO\.(print|println)\("""), "System.out.$1(") }
}

val generateBuildInfo by tasks.registering {
    val outputDir = buildInfoDir
    val baseVersion = Regex("""version = "([0-9.]+)"""").find(File(repoRoot, "build.gradle.kts").readText())!!
        .groupValues[1]
    val commit = providers.exec {
        commandLine("git", "-C", repoRoot.absolutePath, "rev-parse", "--short=12", "HEAD")
    }.standardOutput.asText.map { it.trim() }
    inputs.property("baseVersion", baseVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().asFile.resolve("com/oddlabs/util/BuildInfo.java")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.oddlabs.util;

            public final class BuildInfo {
                public static final String BASE_VERSION = "$baseVersion";
                public static final String VERSION = "$baseVersion.0";
                public static final String FULL_VERSION = "v" + VERSION + "-" + Compatibility.API_VERSION + "." + Compatibility.SIM_VERSION;
                public static final String GIT_COMMIT = "${commit.get()}";
                public static final boolean GIT_DIRTY = false;
                public static final String DESCRIPTION = FULL_VERSION + " (" + GIT_COMMIT + ")";
                private BuildInfo() {}
            }
            """.trimIndent() + "\n"
        )
    }
}

android {
    namespace = "com.oddlabs.tribaltrouble"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.oddlabs.tribaltrouble"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "2.2"
    }

    compileOptions {
        sourceCompatibility = declaredJavaLevel
        targetCompatibility = declaredJavaLevel
    }

    sourceSets["main"].java.directories.add(portDir.get().asFile.path)
    sourceSets["main"].java.directories.add(buildInfoDir.get().asFile.path)
}

tasks.named("preBuild") {
    dependsOn(portSources, generateBuildInfo)
}

afterEvaluate {
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = sourceJavaLevel.toString()
        targetCompatibility = sourceJavaLevel.toString()
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
    }
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("org.joml:joml:1.10.8")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.code-disaster.steamworks4j:steamworks4j:1.10.0")

    // No Android replacement yet for what these provide: audio, stb image and Vorbis decoding, and the last LWJGL core types.
    val lwjglVersion = "3.4.1"
    for (module in listOf("lwjgl", "lwjgl-openal", "lwjgl-stb", "lwjgl-tinyfd")) {
        compileOnly("org.lwjgl:$module:$lwjglVersion")
    }
}
