# Core Concepts & Architecture

Sketchboot works by seamlessly connecting three distinct layers:

1. **Spring AOP (Aspect-Oriented Programming)**
2. **Java 22 FFM (Foreign Function & Memory API)**
3. **Rust Native Crate (`cl-tds`)**

## 1. The Native Layer (Rust Count-Min Sketch)

At the lowest level, Sketchboot uses a Rust library called `cl-tds`. 
A Count-Min Sketch (CMS) is a probabilistic data structure designed to count frequencies of events in massive data streams.

Instead of keeping an exact count for every single user (like a `HashMap<String, Integer>`), a CMS uses a fixed-size 2D array of counters and multiple hash functions.
- **Advantage:** Memory never grows. It stays at exactly 1 MB regardless of whether you track 10 IPs or 10 Million IPs.
- **Trade-off:** There is a tiny, mathematically bounded margin of error (over-counting). In rate-limiting scenarios (like "block at 1000 requests"), dropping a user at 998 instead of 1000 is perfectly acceptable for the massive performance gain.

## 2. The FFM Bridge

Java 22 introduced the final version of the **Foreign Function & Memory API**. 
Sketchboot's `ClTdsNative.java` uses this API to load the native shared library (`.so`, `.dll`, or `.dylib`) and map the C-compatible Rust functions directly to Java MethodHandles.

Unlike older technologies (like JNI or JNA), FFM:
- Does not require writing "glue" C code.
- Is drastically faster for native invocations.
- Handles off-heap memory safely via `Arena`.

## 3. The Spring Boot Auto-Configuration

When you include `sketchboot-spring-boot-starter`, Spring Boot's auto-configuration automatically:
1. Detects your Operating System (Windows, Linux, or macOS).
2. Extracts the correct native binary from the `jar` into a temporary folder.
3. Loads the binary into the JVM.
4. Registers `SketchAspect.java` as a global interceptor for any method annotated with `@Sketch*`.

### Request Lifecycle

1. User sends a HTTP request to `/api/data`.
2. `SketchAspect` intercepts the call before it hits your controller.
3. SpEL (Spring Expression Language) evaluates your dynamic key (e.g., `#request.remoteAddr`).
4. The Key is hashed into a 64-bit integer.
5. `ClTdsSketch.java` passes this hash to the Rust library via FFM.
6. Rust increments the sketch and returns the new frequency count.
7. If the count exceeds your limit, `SketchThresholdException` is thrown (returning a 429 status code).
8. Otherwise, the request proceeds to your logic.
