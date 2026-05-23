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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global Exception Handler for Sketchboot.
 * Maps SketchThresholdException to a custom HTTP 429 Too Many Requests response.
 */
@ControllerAdvice
@ConditionalOnWebApplication
public class SketchExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SketchExceptionHandler.class);

    @ExceptionHandler(SketchThresholdException.class)
    public ResponseEntity<Map<String, Object>> handleSketchLimitBreach(SketchThresholdException ex) {
        logger.warn("Rate Limit Breached: {}", ex.getMessage());

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("timestamp", Instant.now().toString());
        errorBody.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorBody.put("error", "Too Many Requests");
        errorBody.put("message", ex.getMessage());
        errorBody.put("library", "sketchboot-cltds");

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorBody);
    }
}
