# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-23

### Added
- **Native Rust Wrapper (`sketchboot-native`)**: Created a Rust `cdylib` wrapper that securely binds the `cl-tds` crate and exposes an FFI layer for Java, keeping the core crate 100% dependency-free.
- **Java 22 FFM Bindings**: Implemented `ClTdsNative` and `ClTdsSketch` to load and interact with the `.so` library natively without JNI, ensuring ultra-low latency and 1 MB fixed memory usage.
- **7 Spring Boot Annotations**: Added declarative annotations for zero-friction integration: `@SketchLimit`, `@SketchShield`, `@SketchFraud`, `@SketchHitter`, `@SketchSurge`, `@SketchCheat`, and `@SketchSensor`.
- **Unified AOP Interceptor**: Built `SketchAspect` to dynamically parse SpEL expressions (e.g., `#userId`), intercept method calls, and manage sketch pooling based on decay windows.
- **Robust Error & NPE Handling**: SpEL parsing now safely falls back to method signature hashes on errors or null arguments, preventing application crashes.
- **Custom Exception Handling**: Added `SketchExceptionHandler` (@ControllerAdvice) to automatically map `SketchThresholdException` to HTTP 429 (Too Many Requests) with custom JSON payloads.
- **Actuator & Micrometer Metrics**: Exposed `GET /actuator/sketches` endpoint to monitor sketch states, and registered `cltds.query.count` and `cltds.threshold.breach` counters for Grafana/Prometheus.
- **Integration Tests**: Verified robust Java-to-Rust communication via `ClTdsSketchTest`.

### Roadmap (v2.0)
- **Distributed Merge**: Gossip protocol with Rust-driven LZ4 compression for Redis-free distributed merging across nodes.
