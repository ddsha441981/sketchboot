# Batch & Off-Heap Ingestion

Sketchboot is not only designed for individual HTTP rate limiting via annotations. It also provides a low-level, high-throughput ingestion API for background processors, Kafka consumers, and massive data pipelines.

## The Problem with Single FFM Calls

While FFM is incredibly fast (sub-microsecond), crossing the Java-to-Rust boundary millions of times per second in a loop still introduces overhead.
For streaming data systems where you might receive a batch of 50,000 IPs at once from a message queue, iterating and calling `ClTdsNative.SKETCH_INCREMENT` individually is suboptimal.

## The Solution: Batch Ingestion

Sketchboot's `ClTdsSketch` class exposes two advanced methods designed specifically to eliminate FFM boundary overhead for large payloads:

### 1. `incrementBatch(long[] keys)`
If you have an array of Java `long` hashes, you can pass them entirely to Rust in a single FFM crossing.
Under the hood, Sketchboot allocates a temporary confined `Arena`, copies the array into off-heap memory, and tells Rust to process the entire contiguous block at once.

```java
import io.github.ddsha441981.sketchboot.core.ClTdsSketch;

public void processKafkaBatch(long[] userHashes) {
    // sketch is a configured ClTdsSketch instance
    sketch.incrementBatch(userHashes);
}
```

### 2. `incrementBatchOffHeap(MemorySegment keysSegment, long count)`
For the absolute extreme edge cases of performance (e.g., zero-copy parsing engines or direct memory mappers), if your keys are *already* in off-heap memory, you can pass the raw `MemorySegment` directly.
This avoids JVM array allocation completely and allows Rust to directly read the memory chunk.

```java
public void processDirectMemory(MemorySegment keysSegment, long count) {
    sketch.incrementBatchOffHeap(keysSegment, count);
}
```

## When to use these?
- **Annotations (`@SketchLimit`)**: Use for standard HTTP APIs where requests arrive individually.
- **Batch API**: Use when processing massive log files, consuming from Kafka, or acting as an aggregator node where thousands of items are grouped together.
