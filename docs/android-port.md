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

LWJGL 3.4.1 with a GLFW window and an OpenGL 4.1 core context. 102 files import LWJGL, but the
client calls only about 96 distinct GL functions (GL11 up to GL41), which is small enough to
re-implement over `android.opengl.GLES32`, keeping the `org.lwjgl.opengl.GL*` names so call sites
do not change.

Functions or features with no direct GLES equivalent, or that need attention:

| Use | Where | GLES note |
|---|---|---|
| `glPolygonMode` | `DefaultRenderer` (debug wireframe) | Not in ES. Drop or guard. |
| `glDrawBuffer` | `FBO`, `DecalRenderer` | Not in ES. Use `glDrawBuffers`. |
| `glGetTexImage` | `GLUtils` (screenshots) | Not in ES. Read through an FBO with `glReadPixels`. |
| `glClearDepth` | `GLRenderContext` | ES has `glClearDepthf`. |
| `glBindFragDataLocation` | `ShaderProgram` wrapper | Not in ES. The only callers are commented out and shaders use `layout(location)`. |
| `GL_MULTISAMPLE` | `GLRenderContext` | Not valid in ES. MSAA needs a multisampled renderbuffer or an `EGL` config. |
| `glTexBuffer`, `GL_TEXTURE_BUFFER`, `GL_RGB32F` | `SpriteList`, `InstancedSpriteRenderer` | Texture buffers are core in ES 3.2 only. Confirm `GL_RGB32F` is allowed for buffer textures on target devices. |
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
