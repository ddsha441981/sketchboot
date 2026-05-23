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

package io.github.ddsha441981.sketchboot.benchmark;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.ddsha441981.sketchboot.core.ClTdsSketch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Disabled("Run manually for benchmarking memory usage")
public class MemoryBenchmarkTest {

    // Reduced to 5 Million to ensure the 4GB Heap can handle testing ALL tools sequentially
    private static final int USER_COUNT = 5_000_000;

    @Test
    public void runUltimateMemoryComparison() throws Exception {
        System.out.println("==================================================");
        System.out.println("🚀 ULTIMATE BENCHMARK: 5 MILLION USERS");
        System.out.println("Comparing: Bucket4j | Resilience4j | Guava | Caffeine | Sketchboot");
        System.out.println("==================================================");

        String[] users = new String[USER_COUNT];
        for (int i = 0; i < USER_COUNT; i++) {
            users[i] = "u_" + i;
        }

        // ==========================================
        // TEST 1: BUCKET4J
        // ==========================================
        runGc();
        long startMemBucket4j = getUsedMemory();
        ConcurrentHashMap<String, Bucket> bucketMap = new ConcurrentHashMap<>(USER_COUNT);
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        
        System.out.println("\n[1] Pushing 5M users to Bucket4j...");
        long startTimeBucket4j = System.currentTimeMillis();
        for (int i = 0; i < USER_COUNT; i++) {
            bucketMap.put(users[i], Bucket.builder().addLimit(limit).build());
        }
        long timeBucket4j = System.currentTimeMillis() - startTimeBucket4j;
        
        runGc();
        System.out.println("[Bucket4j] Time: " + timeBucket4j + " ms | Memory: " + getMemoryDiffMB(startMemBucket4j) + " MB");
        bucketMap.clear(); bucketMap = null;

        // ==========================================
        // TEST 2: RESILIENCE4J
        // ==========================================
        runGc();
        long startMemResilience = getUsedMemory();
        RateLimiterConfig rConfig = RateLimiterConfig.custom().limitRefreshPeriod(Duration.ofMinutes(1)).limitForPeriod(5).build();
        RateLimiterRegistry registry = RateLimiterRegistry.of(rConfig);
        
        System.out.println("\n[2] Pushing 5M users to Resilience4j...");
        long startTimeResilience = System.currentTimeMillis();
        for (int i = 0; i < USER_COUNT; i++) {
            registry.rateLimiter(users[i]);
        }
        long timeResilience = System.currentTimeMillis() - startTimeResilience;

        runGc();
        System.out.println("[Resilience4j] Time: " + timeResilience + " ms | Memory: " + getMemoryDiffMB(startMemResilience) + " MB");
        registry = null;

        // ==========================================
        // TEST 3: GUAVA RATE LIMITER
        // ==========================================
        runGc();
        long startMemGuava = getUsedMemory();
        ConcurrentHashMap<String, RateLimiter> guavaMap = new ConcurrentHashMap<>(USER_COUNT);

        System.out.println("\n[3] Pushing 5M users to Guava...");
        long startTimeGuava = System.currentTimeMillis();
        for (int i = 0; i < USER_COUNT; i++) {
            guavaMap.put(users[i], RateLimiter.create(5.0)); // 5 per second
        }
        long timeGuava = System.currentTimeMillis() - startTimeGuava;

        runGc();
        System.out.println("[Guava] Time: " + timeGuava + " ms | Memory: " + getMemoryDiffMB(startMemGuava) + " MB");
        guavaMap.clear(); guavaMap = null;

        // ==========================================
        // TEST 4: CAFFEINE CACHE
        // ==========================================
        runGc();
        long startMemCaffeine = getUsedMemory();
        Cache<String, Integer> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).maximumSize(USER_COUNT).build();

        System.out.println("\n[4] Pushing 5M users to Caffeine Cache...");
        long startTimeCaffeine = System.currentTimeMillis();
        for (int i = 0; i < USER_COUNT; i++) {
            cache.put(users[i], 1);
        }
        long timeCaffeine = System.currentTimeMillis() - startTimeCaffeine;

        runGc();
        System.out.println("[Caffeine] Time: " + timeCaffeine + " ms | Memory: " + getMemoryDiffMB(startMemCaffeine) + " MB");
        cache.invalidateAll(); cache = null;

        // ==========================================
        // TEST 5: SKETCHBOOT (Rust FFM)
        // ==========================================
        runGc();
        long startMemSketch = getUsedMemory();

        System.out.println("\n[5] Pushing 5M users to Sketchboot (Rust FFM)...");
        try (ClTdsSketch sketch = new ClTdsSketch(60000)) {
            long startTimeSketch = System.currentTimeMillis();
            for (int i = 0; i < USER_COUNT; i++) {
                sketch.increment(users[i].hashCode());
            }
            long timeSketch = System.currentTimeMillis() - startTimeSketch;

            runGc();
            System.out.println("[Sketchboot] Time: " + timeSketch + " ms | Memory: " + getMemoryDiffMB(startMemSketch) + " MB (Plus fixed 1MB Off-Heap!)");
        }

        System.out.println("\n==================================================");
        System.out.println("🏆 BENCHMARK COMPLETE");
        System.out.println("==================================================");
    }

    private void runGc() throws InterruptedException {
        System.gc();
        Thread.sleep(1500);
    }

    private long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private long getMemoryDiffMB(long startMem) {
        return Math.max(0, getUsedMemory() - startMem) / 1024 / 1024;
    }

    private long measureTime(Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - start;
    }
}
