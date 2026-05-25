# Actuator & System Monitoring

Sketchboot seamlessly integrates with Spring Boot Actuator to provide deep visibility into the native Rust memory structures.

## 1. The `/actuator/sketches` Endpoint

Sketchboot registers a custom Actuator endpoint that allows you to inspect all currently active Count-Min Sketches in your JVM.

**Prerequisites:**
Ensure you have exposed the endpoint in your `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,metrics,sketches"
```

### Fetching All Active Sketches

**Request:**
`GET /actuator/sketches`

**Response:**
```json
{
  "LIMIT_60000": "Active (1 MB)",
  "SHIELD_10000": "Active (1 MB)",
  "FRAUD_300000": "Active (1 MB)"
}
```
*Note:* Sketches are uniquely identified by their annotation type and time window (e.g., `LIMIT_60000` means a `@SketchLimit` with a 60,000ms window).

### Fetching Sketch Details

You can query a specific sketch for its mathematical properties.

**Request:**
`GET /actuator/sketches/LIMIT_60000`

**Response:**
```json
{
  "epsilon": 0.0000414,
  "memory_bytes": 1048576,
  "delta_percent": 1.8
}
```
This tells you that the sketch consumes exactly `1,048,576` bytes (1 MB) of off-heap memory, and has an over-count error bound of ~0.00004.

## 2. Dynamic Library Extraction Logging

When your application boots up, `NativeLibraryLoader` attempts to detect your Operating System (Linux, Windows, or Mac) and architecture.
It extracts the appropriate binary (e.g., `libcl_tds.so`) from the `.jar` to your system's temp directory and loads it.

If you ever encounter `UnsatisfiedLinkError`, check the startup logs for:
```text
INFO: Sketchboot loading native library for OS: linux, Arch: amd64
INFO: Successfully extracted and loaded libcl_tds.so from /tmp/sketchboot_native_12345.so
```
If this fails, it is usually because the `tmp` folder lacks execution permissions or the JVM lacks `--enable-native-access`.
