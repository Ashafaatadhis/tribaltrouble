# Android port: status and plan

This branch starts an Android port of the client. It is a draft prepared by an AI agent and has
**not been built with Gradle or run on a device**. Treat it as a head start for a human to review.

## What exists on this branch

| Piece | State |
|---|---|
| `ShaderDialect` (`tt/.../render/shader`) | Converts the engine's GLSL 4.10 core shaders to GLSL ES 3.20. Off by default; enable with `-Dcom.oddlabs.tt.gl.es=true`. Desktop behaviour is unchanged. |
| `ShaderProgram.compileShader` | Calls `ShaderDialect.adapt` before `glShaderSource`. |
| `PostProcessShader` | `1.0 / textureSize(...)` became `1.0 / vec2(textureSize(...))`. Desktop GLSL allows the implicit conversion, GLSL ES does not. Same result on desktop. |
| `tools/scripts/shader-es/check-shader-es.sh` | Assembles every shader, converts it, and validates the 4.10 and ES versions with `glslangValidator`. |

Verified in a sandbox: `ShaderDialect` compiles with plain `javac` (JDK 21), and all 29 assembled
shaders validate as GLSL 4.10 core and as GLSL ES 3.20 with glslang 11.7.0. Not verified: the
Gradle build (the sandbox could not reach Gradle, Maven Central or the Android SDK), `spotlessCheck`,
and anything at runtime. Validation proves the shaders parse as ES. It does not prove they render
correctly on a GPU.

## Where Android plugs in

The window and input code already sit behind interfaces, so the platform layer is small:

- `com.oddlabs.tt.window.Window` (implemented by `LWJGL3Window`, about 600 lines).
- `com.oddlabs.tt.input.InputProvider<C>` (implemented by `LWJGL3InputProvider`, about 280 lines).
  The engine expects a bottom-left origin for mouse coordinates, so touch input needs a flip.
- `com.oddlabs.tt.audio.AudioManager` has `SilentAudioManager`, so the game can start with no sound
  and get an OpenAL or Android audio backend later.

## OpenGL surface the client uses

LWJGL 3.4.1 with a GLFW window and an OpenGL 4.1 core context. 102 files import LWJGL, but only 40
of them call GL, and they use 92 distinct GL functions (GL11 up to GL41) and 137 distinct `GL_*`
constants. That is small enough to re-implement over `android.opengl.GLES32`, keeping the
`org.lwjgl.opengl.GL*` names so call sites do not change. No file uses `import static` for GL, so
every call goes through a class name such as `GL11.glBindTexture`.

The counts come from a text search of `tt/src/main/java`, not from a compile. The most frequent
calls are `glEnableVertexAttribArray` (30), `glVertexAttribPointer` (27), `glPixelStorei` (26),
`glBindTexture` (20), `glActiveTexture` (15) and `glVertexAttribDivisor` (15). Besides GL the
client uses GLFW (85 imports), OpenAL (61), `MemoryStack` and `MemoryUtil` (16), STB (5) and
TinyFD (2). `LWJGL3Window` calls `GL.createCapabilities()` and `Renderer` reads `GL_VERSION`, which
is where a shim would check the ES version.

Functions or features with no direct GLES equivalent, or that need attention:

| Use | Where | GLES note |
|---|---|---|
| `glPolygonMode` | `DefaultRenderer` (debug wireframe) | Not in ES. Drop or guard. |
| `glDrawBuffer` | `FBO`, `DecalRenderer` | Not in ES. Use `glDrawBuffers`. |
| `glGetTexImage` | `GLUtils` (screenshots) | Not in ES. Read through an FBO with `glReadPixels`. |
| `glClearDepth` | `GLRenderContext` | ES has `glClearDepthf`. |
| `glBindFragDataLocation` | `ShaderProgram` wrapper | Not in ES. The only callers are commented out and shaders use `layout(location)`. |
| `GL_MULTISAMPLE` | `GLRenderContext` | Not valid in ES. MSAA needs a multisampled renderbuffer or an `EGL` config. |
| `GL_QUADS`, `GL_QUAD_STRIP` | `ShortVBO` | Only used to compute an index count, never drawn. Define the constants in the shim. |
| `glGetTexLevelParameteriv` with `GL_TEXTURE_COMPRESSED`, `GL_TEXTURE_COMPRESSED_IMAGE_SIZE` | `Texture` | Used to compute texture sizes. Compute the size on the client instead. |
| `glGetFramebufferAttachmentParameteri` with `GL_BACK_LEFT` | `Renderer` | Default-framebuffer colour depth check. ES uses `GL_BACK`. |
| `glTexParameterfv` with `GL_TEXTURE_BORDER_COLOR` | `Texture` | Border colour is ES 3.2 only. |
| `GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT` | `EXTTextureFilterAnisotropic` | Needs `EXT_texture_filter_anisotropic`. Check the extension at startup. |
| `glTexBuffer`, `GL_TEXTURE_BUFFER`, `GL_RGB32F` | `SpriteList`, `InstancedSpriteRenderer` | Texture buffers are core in ES 3.2 only. `GL_RGB32F` does not appear to be a valid buffer-texture format in ES 3.2 (`R32F`, `RG32F` and `RGBA32F` are); check the spec and probably switch to `RGBA32F`. |
| `glBlendFunci` | `PostProcessor` | Per-draw-buffer blend is ES 3.2. |
| `GL_R32F`, `GL_RGBA16F` | `Texture`, `FBO`, `HeightMap` | Rendering to float formats needs float color-buffer support (ES 3.2 or `EXT_color_buffer_float`). Confirm on target devices. |
| `GL_LUMINANCE`, `GL_ALPHA`, generic `GL_COMPRESSED_*`, `GL_BGRA` with `GL_UNSIGNED_INT_8_8_8_8_REV` | `GLImage`, `GLByteImage`, `Texture` | Legacy or desktop-only formats. Convert on the CPU or pick sized ES formats. |
| DXT1 / DXT5 textures (`TextureFile`, `EXTTextureCompressionS3TC`) | texture loading | S3TC is not available on every Android GPU. Plan to decode DXT to RGBA on load, or transcode to ETC2 or ASTC at asset build time. |
| Geometry shader | `ParticleShader` | Core in ES 3.2 only, and slow on some mobile GPUs. A later change could replace it with instancing. |

Target ES 3.2 first. It covers most rows above, and the app can check the version at startup.

## Toolchain

- The build targets **Java 26** with JPMS modules (`module-info.java` in `common`, `tt`, `tools`).
  Android toolchains support a much lower language level, so the client will need a Java level
  decision. The code uses `record` in about 57 files, `var` in about 117 and `switch` in about 98,
  and one `sealed` type. Check each against the level you choose.
- Java level, measured in a throwaway spike (AGP 9.2.1, Gradle 9.4.1, JDK 26 running the build,
  `compileSdk` 37, outside this repository):
  - Gradle 9.4 to 9.6 run on JDK 26. AGP 9.2 needs Gradle 9.4.1, which is what the wrapper uses.
  - AGP's bundled Kotlin rejects Java 26 (`Unknown Kotlin JVM target: 26`) and stops at 24, so
    `compileOptions` has to say 24 or lower.
  - `javac` can still run at `-source 25 -target 25` by overriding `sourceCompatibility` and
    `targetCompatibility` on the `JavaCompile` tasks in `afterEvaluate`. D8 accepted class file
    version 69, including a flexible constructor (statements before `super(...)`). The override is
    a workaround and may break on a newer AGP.
  - Level 25 is needed: `tt` uses flexible constructors in `KeyboardEvent`, `IconQuad`, `GUIImage`,
    `Cursor` and `MapCamera`. Records, sealed types, pattern `switch` and unnamed variables (`_`)
    all compile and dex.
  - `java.io.IO` (Java 25) is not in `android.jar`. The 128 `IO.print*` calls in 32 files need
    `System.out` or a shim.
  - Other desktop-only APIs that failed to compile: `java.awt` and `java.awt.datatransfer`
    (`EditLine`, `Menu`, `Settings`, `AccessibilityPanel`, `procedural/Layer`), `javax.imageio`
    (`Layer`), `javax.sound.sampled` (`Wave`, `OpenALAudio`), `java.net.http` (`BugReporter`) and
    `java.io.ObjectInputFilter` (`MatchmakingClient`, `DefaultARMIArgumentReader`). The
    `ObjectInputFilter` files sit on the network path, so replacing them is a maintainer decision.
  - `FloatBuffer.flip()` and `rewind()` return `Buffer` against `android.jar` (`Sky`,
    `PostProcessor`, `Utils`), so those chains need a cast or a separate statement.
  - Not measured: whether `tt` has further errors behind these (javac reports in stages), and
    anything at runtime. The 101 `tt` files that import LWJGL were compiled against LWJGL on the
    classpath, which is not an Android option.
- Remove or replace desktop-only dependencies for Android: LWJGL natives, GLFW, TinyFD,
  Steamworks4j, and `java.awt` (3 files: `EditLine`, `Menu`, `procedural/Layer`).
- `assets/` is the art source (about 254 MB) that Gradle tasks convert to the binary formats. Decide
  what ships in the APK or an asset pack.

## Rules from the project that apply here

- Do not change simulation logic or wire formats. The multiplayer population depends on both.
- No AI-generated art, sound or translations (see `AGENTS.md`). Agent-written code is a draft until
  a human reviews it.
- The result is GPL v2, so a distributed port must stay open source.

## Suggested order

1. Build the desktop client and confirm `-Dcom.oddlabs.tt.gl.es=true` is harmless on desktop.
2. Add a GL shim (`org.lwjgl.opengl.GL*` over `GLES32`) in a new `android` module, with the gaps
   above handled in one place.
3. Implement `Window` and `InputProvider` for Android (a `GLSurfaceView` or `EGL` surface, touch
   input mapped to the existing pointer and keyboard events).
4. Get the menu rendering, then the game scene, with `SilentAudioManager`.
5. Design touch controls (selection, camera, hotkeys), then audio.
