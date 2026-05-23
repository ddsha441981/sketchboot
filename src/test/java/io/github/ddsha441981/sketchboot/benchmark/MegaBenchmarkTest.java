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

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import dev.failsafe.RateLimiter;
import org.apache.datasketches.frequencies.ItemsSketch;
import io.github.ddsha441981.sketchboot.core.ClTdsSketch;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class MegaBenchmarkTest {

    private static final int USER_COUNT = 5_000_000;

    @Test
    public void runMegaComparison() throws Exception {
        System.out.println("==================================================");
        System.out.println("🔥 MEGA BENCHMARK: 5 MILLION USERS");
        System.out.println("Testing: Sentinel (Alibaba) | Apache DataSketches | Failsafe | Sketchboot");
        System.out.println("==================================================");

        String[] users = new String[USER_COUNT];
        for (int i = 0; i < USER_COUNT; i++) {
            users[i] = "u_" + i;
        }

        // ==========================================
        // TEST 1: ALIBABA SENTINEL
        // ==========================================
        runGc();
        long startMemSentinel = getUsedMemory();
        System.out.println("\n[1] Pushing 5M users to Alibaba Sentinel...");
        
        FlowRule rule = new FlowRule();
        rule.setResource("mega_test");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(5);
        FlowRuleManager.loadRules(Collections.singletonList(rule));

        long startTimeSentinel = System.currentTimeMillis();
        for (int i = 0; i < USER_COUNT; i++) {
            try (Entry entry = SphU.entry(users[i])) {
                // Allowed
            } catch (Exception ex) {
                // Blocked
            }
        }
        long timeSentinel = System.currentTimeMillis() - startTimeSentinel;
        
        runGc();
        System.out.println("[Alibaba Sentinel] Time: " + timeSentinel + " ms | Memory: " + getMemoryDiffMB(startMemSentinel) + " MB");

        // ==========================================
        // TEST 2: APACHE DATASKETCHES (Yahoo)
        // ==========================================
        runGc();
        long startMemApache = getUsedMemory();
        System.out.println("\n[2] Pushing 5M users to Apache DataSketches (Java)...");
        
        // Exact same algorithm as Sketchboot, but written in Java Instead of Rust
        ItemsSketch<String> apacheSketch = new ItemsSketch<>(65536);

        long startTimeApache = System.currentTimeMillis();
        for (int i = 0; i < USER_COUNT; i++) {
            apacheSketch.update(users[i]);
        }
        long timeApache = System.currentTimeMillis() - startTimeApache;

        runGc();
        System.out.println("[Apache DataSketches] Time: " + timeApache + " ms | Memory: " + getMemoryDiffMB(startMemApache) + " MB");
        apacheSketch = null;

        // ==========================================
        // TEST 3: FAILSAFE
        // ==========================================
        runGc();
        long startMemFailsafe = getUsedMemory();
        System.out.println("\n[3] Pushing 5M users to Failsafe...");
        
        ConcurrentHashMap<String, RateLimiter<Object>> failsafeMap = new ConcurrentHashMap<>(USER_COUNT);
        
        long startTimeFailsafe = System.currentTimeMillis();
        for (int i = 0; i < USER_COUNT; i++) {
            failsafeMap.put(users[i], RateLimiter.smoothBuilder(5, Duration.ofSeconds(1)).build());
        }
        long timeFailsafe = System.currentTimeMillis() - startTimeFailsafe;

        runGc();
        System.out.println("[Failsafe] Time: " + timeFailsafe + " ms | Memory: " + getMemoryDiffMB(startMemFailsafe) + " MB");
        failsafeMap.clear(); failsafeMap = null;

        // ==========================================
        // TEST 4: SKETCHBOOT (Rust FFM)
        // ==========================================
        runGc();
        long startMemSketch = getUsedMemory();

        System.out.println("\n[4] Pushing 5M users to Sketchboot (Rust FFM)...");
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
        System.out.println("🏆 MEGA BENCHMARK COMPLETE");
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
}
