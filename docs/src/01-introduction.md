# Introduction to Sketchboot

**Version:** `1.0.0`

Welcome to the official documentation for **Sketchboot**, the ultra-fast, fixed-memory (1 MB) rate limiting and heavy-hitter detection starter for Spring Boot 3.x.

## What is Sketchboot?

Sketchboot is a bridge between the blazing-fast performance of **Rust** and the enterprise robustness of **Java**. By utilizing the cutting-edge **Java 22 Foreign Function & Memory (FFM) API**, Sketchboot embeds a Rust-based Count-Min Sketch (`cl-tds`) directly into your Spring Boot application's native memory.

### Why was Sketchboot built?

Traditional rate limiters (like Redis, Caffeine, or Bucket4j) often suffer from:
1. **Memory Bloat:** The more unique users or IP addresses you track, the larger the memory footprint grows. A million unique IPs can consume hundreds of megabytes of JVM Heap.
2. **Garbage Collection Pauses:** High object creation (like tracking maps or buckets) puts pressure on the Java Garbage Collector, causing latency spikes in production.
3. **Network Latency:** External rate limiters like Redis require a network hop for every single request.

### The Sketchboot Advantage

- **Fixed O(1) Memory:** Sketchboot uses a probabilistic data structure (Count-Min Sketch). Whether you have 10 users or 100 Million users, the memory footprint remains locked at exactly **1 Megabyte**.
- **Zero JVM Heap:** The state is allocated off-heap using the FFM API. This means **zero Garbage Collection** pressure.
- **Microsecond Latency:** Rate limits are checked natively in RAM without any network calls, taking only ~0.005 milliseconds per request.
- **Cross-Platform:** Sketchboot ships with pre-compiled native binaries for Linux (`.so`), Windows (`.dll`), and macOS (`.dylib`). It works automatically out of the box.

In the next sections, we will explore how to install and use Sketchboot.
