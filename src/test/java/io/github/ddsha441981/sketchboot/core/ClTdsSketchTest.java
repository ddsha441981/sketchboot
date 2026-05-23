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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClTdsSketchTest {

    @Test
    public void testNativeSketchConnection() {
        System.out.println("Connecting to Rust native library...");
        
        // Use 0 for manual mode (no decay) to keep tests predictable
        try (ClTdsSketch sketch = new ClTdsSketch(0)) {
            System.out.println("Sketch initialized successfully!");
            
            long testUserId = 999111L;
            
            // Check initial state
            assertEquals(0, sketch.query(testUserId), "Initial count should be 0");
            
            // Increment 5 times
            for (int i = 0; i < 5; i++) {
                sketch.increment(testUserId);
            }
            
            // Because it's Count-Min Sketch, it guarantees >= actual count
            int count = sketch.query(testUserId);
            System.out.println("Queried count for user " + testUserId + ": " + count);
            
            assertTrue(count >= 5, "Count should be at least 5, but got " + count);
            System.out.println("Native integration test passed! ✅");
        }
    }
}
