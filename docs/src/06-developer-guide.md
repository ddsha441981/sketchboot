# Developer & Contribution Guide

If you are a developer looking to contribute to the Sketchboot open-source project, or if you simply want to build it from source, follow this guide.

## Project Structure

The project is split into two massive ecosystems:
1. **The Native Library (Rust):** The core Count-Min Sketch logic is written in Rust. It compiles to a shared dynamic library.
2. **The Spring Boot Starter (Java):** The Java side uses the FFM API to invoke the Rust shared library, wrapped in an easy-to-use Spring Boot AutoConfiguration.

```
sketchboot/
├── docs/                        # mdBook style documentation
├── rust-cl-tds/                 # (External/Included) The Rust Source Code
├── src/
│   ├── main/java/.../sketchboot # The Core Java FFM implementation
│   │   ├── annotation/          # @SketchLimit, @SketchShield, etc.
│   │   ├── aop/                 # Spring Aspect (SketchAspect.java)
│   │   └── core/                # FFM Bindings (ClTdsNative.java)
│   └── main/resources/          
│       └── natives/             # Pre-compiled OS binaries (.so, .dll, .dylib)
└── pom.xml                      # Maven Build File
```

## Building from Source

### 1. Requirements
- JDK 22+
- Maven 3.8+
- Cargo & Rust toolchain (If you are modifying the C-bindings)

### 2. Building the Native Binaries (Optional)
If you make changes to the Rust code, you must recompile for your target OS:
```bash
cargo build --release
```
Then copy the resulting binary (`libcl_tds.so`, `cl_tds.dll`, or `libcl_tds.dylib`) into `src/main/resources/natives/`.

### 3. Compiling the Java Project
To compile the Spring Boot Starter:
```bash
mvn clean install -DskipTests
```
This will build `sketchboot-spring-boot-starter-1.0.0.jar` and install it into your local Maven cache (`~/.m2`).

## Running Tests

Sketchboot comes with an extensive testing suite covering unit tests, integration tests, and simulated heavy-traffic (GodMode) benchmarks.

```bash
mvn test
```
*Note: Because tests invoke the native library via FFM, Maven Surefire plugin is configured to run with `--enable-native-access=ALL-UNNAMED`.*

## Modifying the FFM Bindings

If the Rust `extern "C"` signature changes, you must update `ClTdsNative.java`.
Currently, the signature maps to:
```rust
#[no_mangle]
pub extern "C" fn increment_sketch(key: u64) -> u64
```
Which is mapped in Java using `Linker.nativeLinker()` and `FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)`.

## Publishing a Release

Sketchboot relies on a GitHub Actions CI/CD pipeline (`.github/workflows/publish.yml`). 
When a new release tag is pushed, it automatically signs the artifact with GPG and uploads it to Sonatype Maven Central (OSSRH).
Make sure to remove `-SNAPSHOT` from `pom.xml` before pushing a release.
