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

package io.github.ddsha441981.sketchboot.exception;

/**
 * Exception thrown when a Sketchboot rate limit threshold is breached.
 * Automatically handled by SketchExceptionHandler if auto-configured.
 */
public class SketchThresholdException extends RuntimeException {
    
    public SketchThresholdException(String message) {
        super(message);
    }
}
