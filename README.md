# Sketchboot

[![Java 22](https://img.shields.io/badge/Java-22-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Rust](https://img.shields.io/badge/Rust-Native-orange.svg)](https://www.rust-lang.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ddsha441981/sketchboot-spring-boot-starter)](https://search.maven.org/artifact/io.github.ddsha441981/sketchboot-spring-boot-starter)
[![Documentation](https://img.shields.io/badge/Docs-mdBook-orange.svg)](https://ddsha441981.github.io/sketchboot/)



**📖 Read the full mdBook Documentation here: [https://ddsha441981.github.io/sketchboot/](https://ddsha441981.github.io/sketchboot/)**

A fixed-memory rate limiting starter for Spring Boot 3.x, backed by a Rust-based Count-Min Sketch.

Sketchboot uses the Java 22 Foreign Function & Memory (FFM) API to interface with a native Rust library (`cl-tds`). It provides rate limiting capabilities without requiring external caching servers, while maintaining a fixed memory footprint regardless of the number of tracked entities.

## Technical Characteristics

*   **Fixed Memory Usage**: Allocates a fixed 1 MB off-heap memory segment per sketch. It does not store individual keys, avoiding heap allocation scaling issues.
*   **Concurrency**: Uses lock-free atomic Compare-And-Swap (CAS) operations in Rust. It does not use Java-level synchronization or locks.
*   **Probabilistic Accuracy**: Built on the Count-Min Sketch data structure. It guarantees no undercounting, but has a known mathematical probability of overcounting (false positives) due to hash collisions. It is not suitable for exact accounting or billing.
*   **Predictable Latency**: Native execution avoids Java JIT compiler warmup variance.
*   **Integration**: Provides 7 declarative Spring annotations with SpEL support for key extraction.

## Why Sketchboot? (The Architectural Trade-offs)

When dealing with massive scale (e.g., 100 million unique users or IPs), traditional limiters face architectural limits:

*   **Local Map Limiters (Bucket4j, Guava, Resilience4j):** They provide exact accuracy but suffer from linear memory growth. Storing 100 million state objects in a `ConcurrentHashMap` requires 15GB+ of Java heap, causing severe Garbage Collection pauses or `OutOfMemoryError`. Sketchboot uses fixed memory (1 MB).
*   **Distributed Limiters (Redis + Lua):** Highly accurate and scalable across instances, but introduce network latency and high infrastructure costs for large RAM clusters. Sketchboot runs locally at nanosecond latency via native FFM calls.
*   **Local Caching (Caffeine):** Prevents OOM by evicting older data. However, under a heavy DDoS attack, attacker IPs flood the cache, forcing legitimate users to be evicted and forgotten (Amnesia). Sketchboot never evicts keys; it safely degrades under extreme saturation.
*   **API Gateways:** Excellent for generic IP blocking, but they lack deep application context. Sketchboot's SpEL integration (`@SketchFraud(key = "#payment.cardNumber")`) allows for complex, domain-specific rate limiting directly at the controller level.

## Security & Consistency Advantages

1. **HashDoS Resistance**: Sketchboot's underlying Rust engine uses `AHash`, a high-performance hash function designed to resist HashDoS attacks where attackers intentionally craft input to cause hash collisions.
2. **Predictable P99 Latency**: Java-based sketches (like Apache DataSketches) are subject to JVM JIT (Just-In-Time) compiler warmup. Their latency can fluctuate significantly during sudden traffic spikes. Because Sketchboot relies on an Ahead-Of-Time (AOT) compiled Rust library, it delivers consistent execution speed on every run.

## Benchmark Results (100 Million Operations)

Testing environment: 100 million insertions, single machine, comparing heap-based state storage vs off-heap sketch.

| Metric | Map-based Limiters (e.g. Bucket4j) | Sketchboot |
| :--- | :--- | :--- |
| **Data Structure** | `ConcurrentHashMap` | Off-Heap `AtomicU32` Array |
| **Memory at 100M Keys** | ~15 GB+ (Heap) | 1 MB (Off-Heap) |
| **Multi-threaded Execution** | Synchronized locks or Concurrent data structures | Lock-free Atomics |
| **Garbage Collection** | High object churn | Zero heap allocations |

*(Note: If precise counting is required for business logic, use a database or map-based limiter. Sketchboot trades precision for fixed memory bounds.)*

## Installation

Requires **Java 22** and **Spring Boot 3.x**.

Add the dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>io.github.ddsha441981</groupId>
    <artifactId>sketchboot-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Important**: You must start the JVM with `--enable-native-access=ALL-UNNAMED` to allow the FFM API to load the native library.

## Usage 

Sketchboot provides 7 annotations to apply rate limits at the controller or method level.

### 1. @SketchLimit (General Rate Limiting)
```java
@SketchLimit(requests = 5, windowMs = 10000, key = "#userId")
@GetMapping("/data")
public String fetchData(@RequestParam String userId) { ... }
```

### 2. @SketchShield (Brute Force Protection)
```java
@SketchShield(threshold = 3, windowMs = 60000, key = "#ipAddress")
@PostMapping("/login")
public String login(@RequestHeader("X-Forwarded-For") String ipAddress) { ... }
```

### 3. @SketchFraud (Fraud Detection)
```java
@SketchFraud(maxEvents = 2, windowMs = 300000, key = "#payment.cardNumber")
@PostMapping("/payment")
public String makePayment(@RequestBody PaymentRequest payment) { ... }
```

### 4. @SketchHitter (Heavy Hitter Detection)
```java
@SketchHitter(threshold = 100, windowMs = 60000, key = "#streamId")
@GetMapping("/download/{streamId}")
public void downloadFile(@PathVariable String streamId) { ... }
```

### 5. @SketchSurge (Webhook Surge Protection)
```java
@SketchSurge(maxErrors = 50, windowMs = 60000, key = "#webhookSource")
@PostMapping("/webhook")
public void handleWebhook(@RequestParam String webhookSource) { ... }
```

### 6. @SketchCheat (Action Rate Monitoring)
```java
@SketchCheat(maxActions = 10, windowMs = 1000, key = "#playerId")
@PostMapping("/game/action")
public void playerAction(@RequestParam String playerId) { ... }
```

### 7. @SketchSensor (IoT Burst Protection)
```java
@SketchSensor(maxAlerts = 20, windowMs = 60000, key = "#sensorId")
@PostMapping("/telemetry")
public void recordData(@RequestParam String sensorId) { ... }
```

## Error Handling

When a threshold is exceeded, a `SketchThresholdException` is thrown.
If auto-configuration is enabled, the library returns an HTTP 429 status code with the following JSON structure:

```json
{
  "timestamp": "2026-05-23T12:00:00.000Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "[FRAUD] Limit exceeded! Key Hash: 665655120 | Current: 3 | Max Allowed: 2",
  "library": "sketchboot-cltds"
}
```

## Actuator and Metrics

- **Endpoint**: `GET /actuator/sketches` returns the active sketch pools and configuration parameters.
- **Metrics**: Registers `cltds.query.count` and `cltds.threshold.breach` in the Micrometer registry.

## Developer

**Deendayal Kumawat**
- **GitHub**: [ddsha441981](https://github.com/ddsha441981)
- **LinkedIn**: [Deendayal Kumawat](https://www.linkedin.com/in/deendayal-kumawat/)
- **Dev.to**: [ddsha441981](https://dev.to/ddsha441981)
- **Medium**: [@ddsha441981](https://medium.com/@ddsha441981)

## Changelog

See the [CHANGELOG.md](CHANGELOG.md) for detailed release notes and version history.

## License

This project is licensed under the Apache License, Version 2.0.
You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).
