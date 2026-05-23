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

package io.github.ddsha441981.sketchboot.actuator;

import io.github.ddsha441981.sketchboot.aop.SketchAspect;
import io.github.ddsha441981.sketchboot.core.ClTdsSketch;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot Actuator Endpoint for Sketchboot.
 * Exposes /actuator/sketches and /actuator/sketches/{name}
 */
@Component
@Endpoint(id = "sketches")
public class SketchEndpoint {

    private final SketchAspect sketchAspect;

    public SketchEndpoint(SketchAspect sketchAspect) {
        this.sketchAspect = sketchAspect;
    }

    @ReadOperation
    public Map<String, Object> getAllSketches() {
        Map<String, Object> stats = new HashMap<>();
        sketchAspect.getSketchPool().forEach((name, sketch) -> {
            stats.put(name, "Active (1 MB)");
        });
        
        if (stats.isEmpty()) {
            stats.put("status", "No sketches active yet.");
        }
        
        return stats;
    }

    @ReadOperation
    public Map<String, Object> getSketchDetails(@Selector String name) {
        ClTdsSketch sketch = sketchAspect.getSketchPool().get(name);
        if (sketch == null) {
            return Map.of("error", "Sketch not found. Name must match 'PREFIX_windowMs' (e.g., LIMIT_60000)");
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("memory_bytes", 1048576); // Fixed 1 MB matrix
        details.put("epsilon", 0.0000414);    // Overcount error bound
        details.put("delta_percent", 1.8);    // False positive rate
        
        return details;
    }
}
