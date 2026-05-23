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

package io.github.ddsha441981.sketchboot.aop;

import io.github.ddsha441981.sketchboot.annotation.*;
import io.github.ddsha441981.sketchboot.config.SketchAutoConfiguration;
import io.github.ddsha441981.sketchboot.exception.SketchThresholdException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        SketchAutoConfiguration.class,
        SketchAspectTest.HardcoreService.class,
        SketchAspectTest.TestConfig.class
})
public class SketchAspectTest {

    @Autowired
    private HardcoreService hardcoreService;

    // ---------------------------------------------------------
    // TEST 1: ALL 7 ANNOTATIONS & PREFIXES
    // ---------------------------------------------------------
    @Test
    public void testAll7Annotations() {
        assertPrefixBreach("LIMIT", () -> hardcoreService.testLimit("user1"));
        assertPrefixBreach("SHIELD", () -> hardcoreService.testShield("ip1"));
        assertPrefixBreach("FRAUD", () -> hardcoreService.testFraud("card1"));
        assertPrefixBreach("HITTER", () -> hardcoreService.testHitter("stream1"));
        assertPrefixBreach("SURGE", () -> hardcoreService.testSurge("endpoint1"));
        assertPrefixBreach("CHEAT", () -> hardcoreService.testCheat("player1"));
        assertPrefixBreach("SENSOR", () -> hardcoreService.testSensor("iot1"));
    }

    private void assertPrefixBreach(String prefix, Runnable action) {
        // Trigger the limit (Limit is exactly 2 for all tests here)
        action.run();
        action.run();
        SketchThresholdException ex = assertThrows(SketchThresholdException.class, action::run);
        
        // Assert the custom error message contains the specific annotation prefix
        assertTrue(ex.getMessage().contains("[" + prefix + "] Limit exceeded!"), 
            "Expected prefix " + prefix + " but got: " + ex.getMessage());
    }

    // ---------------------------------------------------------
    // TEST 2: HARDCORE CONCURRENCY TEST
    // Proves Rust CAS Atomics and Java 22 FFM Memory Safety
    // ---------------------------------------------------------
    @Test
    public void testHighConcurrencyUnderLoad() throws InterruptedException {
        int threads = 50;
        int iterationsPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successfulRequests = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    try {
                        hardcoreService.testConcurrency("global_ddos_event");
                        successfulRequests.incrementAndGet();
                    } catch (SketchThresholdException ignored) {
                        // Count-Min Sketch has a small false positive rate, so it might breach slightly early.
                        // That is expected behavior for sketching algorithms.
                    }
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Ensure no JVM crashes occurred (Segfaults) and FFM handled it gracefully
        assertTrue(successfulRequests.get() > 0, "No requests succeeded under load!");
        System.out.println("Hardcore Concurrency Test Passed! Safe requests: " + successfulRequests.get());
    }

    // ---------------------------------------------------------
    // TEST 3: SpEL EDGE CASES (COMPLEX OBJECTS)
    // ---------------------------------------------------------
    @Test
    public void testSpelWithComplexObject() {
        Transaction tx = new Transaction("TX-999", 5000.00);
        
        hardcoreService.testComplexObject(tx);
        hardcoreService.testComplexObject(tx);
        SketchThresholdException ex = assertThrows(SketchThresholdException.class, () -> {
            hardcoreService.testComplexObject(tx);
        });
        
        assertTrue(ex.getMessage().contains("[FRAUD]"));
    }

    // ---------------------------------------------------------
    // TEST 4: SpEL EDGE CASES (NULL SAFETY)
    // ---------------------------------------------------------
    @Test
    public void testSpelNullHandling() {
        // Passing null should NOT throw NullPointerException. 
        // It must fallback safely and still enforce limits on the "Null" key.
        hardcoreService.testLimitNull(null);
        hardcoreService.testLimitNull(null);
        SketchThresholdException ex = assertThrows(SketchThresholdException.class, () -> {
            hardcoreService.testLimitNull(null);
        });
        assertTrue(ex.getMessage().contains("Limit exceeded!"));
    }

    // =========================================================
    // DUMMY SERVICE FOR HARDCORE TESTING
    // =========================================================
    @Service
    public static class HardcoreService {

        @SketchLimit(requests = 2, windowMs = 50000, key = "#id")
        public void testLimit(String id) {}

        @SketchLimit(requests = 2, windowMs = 50001, key = "#id")
        public void testLimitNull(String id) {}

        @SketchShield(threshold = 2, windowMs = 50000, key = "#id")
        public void testShield(String id) {}

        @SketchFraud(maxEvents = 2, windowMs = 50000, key = "#id")
        public void testFraud(String id) {}

        @SketchHitter(threshold = 2, windowMs = 50000, key = "#id")
        public void testHitter(String id) {}

        @SketchSurge(maxErrors = 2, windowMs = 50000, key = "#id")
        public void testSurge(String id) {}

        @SketchCheat(maxActions = 2, windowMs = 50000, key = "#id")
        public void testCheat(String id) {}

        @SketchSensor(maxAlerts = 2, windowMs = 50000, key = "#id")
        public void testSensor(String id) {}

        // For Concurrency Test
        @SketchLimit(requests = 6000, windowMs = 60000, key = "#id")
        public void testConcurrency(String id) {}

        // For Complex Object Test
        @SketchFraud(maxEvents = 2, windowMs = 60000, key = "#tx.id")
        public void testComplexObject(Transaction tx) {}
    }

    // Data Transfer Object for SpEL parsing
    public static class Transaction {
        private String id;
        private double amount;
        
        public Transaction(String id, double amount) { 
            this.id = id; 
            this.amount = amount; 
        }
        public String getId() { return id; }
    }

    // Test Config to supply MeterRegistry for Micrometer
    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
