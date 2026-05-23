/*
 * Copyright (c) 2024-2026 Deendayal Kumawat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ddsha441981.sketchboot.core;

import java.lang.foreign.MemorySegment;

/**
 * A clean, object-oriented Java wrapper around the native Rust ClTds engine.
 * No pointers or memory segments are exposed to the end user.
 */
public class ClTdsSketch implements AutoCloseable {

    // The raw pointer to the Rust sketch memory (1 MB)
    private final MemorySegment ptr;

    /**
     * Creates a new Sketch instance.
     * @param intervalMs The decay interval in milliseconds. (e.g., 60000 for 1 minute).
     *                   Use 0 for manual mode.
     */
    public ClTdsSketch(long intervalMs) {
        try {
            this.ptr = (MemorySegment) ClTdsNative.SKETCH_NEW.invokeExact(intervalMs);
            if (this.ptr.address() == 0) {
                throw new IllegalStateException("Rust returned a null pointer during sketch allocation.");
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to allocate native Sketch", e);
        }
    }

    /**
     * Records one occurrence of the given ID in the stream.
     * Operations are atomic and lock-free in Rust.
     */
    public void increment(long id) {
        try {
            ClTdsNative.SKETCH_INCREMENT.invokeExact(this.ptr, id);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to increment sketch", e);
        }
    }

    /**
     * Increments the count for an array of key hashes in a single FFM boundary crossing.
     * This eliminates FFI overhead for large batches.
     */
    public void incrementBatch(long[] keys) {
        if (keys == null || keys.length == 0) return;
        try (var arena = java.lang.foreign.Arena.ofConfined()) {
            MemorySegment segment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_LONG, keys);
            ClTdsNative.SKETCH_INCREMENT_BATCH.invokeExact(this.ptr, segment, (long) keys.length);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke sketch_increment_batch via FFM", e);
        }
    }

    /**
     * Increments the count for an array of key hashes directly from an off-heap MemorySegment.
     * This avoids Java heap allocation entirely for massive scale ingestion.
     *
     * @param keysSegment The MemorySegment pointing to an array of 64-bit integers.
     * @param count The number of elements in the array.
     */
    public void incrementBatchOffHeap(MemorySegment keysSegment, long count) {
        if (keysSegment == null || count <= 0) return;
        try {
            ClTdsNative.SKETCH_INCREMENT_BATCH.invokeExact(this.ptr, keysSegment, count);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke sketch_increment_batch via FFM", e);
        }
    }

    /**
     * Queries the estimated frequency of the given ID.
     */
    public int query(long id) {
        try {
            return (int) ClTdsNative.SKETCH_QUERY.invokeExact(this.ptr, id);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to query sketch", e);
        }
    }

    /**
     * Cleans up the native memory allocated by Rust.
     * Failure to call this will result in a 1 MB memory leak per sketch.
     */
    @Override
    public void close() {
        try {
            if (this.ptr.address() != 0) {
                ClTdsNative.SKETCH_FREE.invokeExact(this.ptr);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to free native Sketch memory", e);
        }
    }
}
