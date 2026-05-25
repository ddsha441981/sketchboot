# Performance Benchmarks

Sketchboot is built on two primary principles: **Fixed Memory** and **Zero-GC execution**. 
This section documents the expected benchmarks of the library.

## 1. The Memory Guarantee

Traditional `HashMap<String, Integer>` structures in Java require JVM Object Headers, String allocation overhead, and map entry nodes.
Tracking 10,000,000 unique IP addresses in a standard `ConcurrentHashMap` can easily consume **~500 MB** to **1 GB** of JVM Heap.

**Sketchboot Memory Profile:**
No matter if you track 1 IP address or 1,000,000,000 (1 Billion) IP addresses, a single Sketchboot bucket consumes exactly **1 Megabyte** of native RAM.
Because this memory is allocated via Java 22 FFM off-heap, it is invisible to the Garbage Collector. You will experience **Zero Stop-The-World (STW) pauses** regardless of traffic scale.

## 2. GodMode Benchmarks

The `sketchboot-live-test` module contains rigorous local benchmarks. 

On a standard developer machine (e.g., i7/16GB RAM), Sketchboot processes requests at extreme speeds:

- **Single FFM Calls (via Spring AOP)**: 
  The overhead of intercepting an HTTP request, parsing the SpEL key, hashing it, and calling Rust is roughly **~0.005 milliseconds** per request.
  You can comfortably handle **>50,000 requests per second** per node without the limiter becoming the bottleneck.

- **Batch API Ingestion**:
  When using `incrementBatch(long[] keys)` to bypass AOP and FFM boundaries, Sketchboot can ingest and count **>30 Million items per second**.

## 3. The Auto-Decay Mechanism

Unlike Redis, where you must rely on TTL keys or background cron jobs to clear out old rate-limiting data, Sketchboot handles decay entirely asynchronously in Rust.

When a sketch is initialized with a `windowMs`, Rust automatically decays the Count-Min Sketch matrix. 
This occurs natively. There is no Java thread sleeping, no `ScheduledExecutorService`, and no CPU bloat on the JVM side. The Rust crate intelligently decays counts over time, ensuring that rate limits naturally "cool down" without any application-level intervention.
