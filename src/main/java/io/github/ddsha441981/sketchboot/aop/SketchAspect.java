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
import io.github.ddsha441981.sketchboot.core.ClTdsSketch;
import io.github.ddsha441981.sketchboot.exception.SketchThresholdException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The core AOP Aspect that intercepts all @Sketch* annotations in the Spring Boot application.
 * It manages the lifecycle of the native cl-tds sketches (allocated via Java 22 FFM) and 
 * enforces rate limits using Spring Expression Language (SpEL) to resolve dynamic keys.
 * 
 * This class ensures that all threshold evaluations are lock-free and highly performant.
 */
@Aspect
@Component
public class SketchAspect {

    private final ConcurrentHashMap<String, ClTdsSketch> sketchPool = new ConcurrentHashMap<>();
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public SketchAspect(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public ConcurrentHashMap<String, ClTdsSketch> getSketchPool() {
        return sketchPool;
    }

    @Around("@annotation(ann)")
    public Object enforceLimit(ProceedingJoinPoint joinPoint, SketchLimit ann) throws Throwable {
        return process(joinPoint, "LIMIT", ann.windowMs(), ann.key(), ann.requests());
    }

    @Around("@annotation(ann)")
    public Object enforceShield(ProceedingJoinPoint joinPoint, SketchShield ann) throws Throwable {
        return process(joinPoint, "SHIELD", ann.windowMs(), ann.key(), ann.threshold());
    }

    @Around("@annotation(ann)")
    public Object enforceFraud(ProceedingJoinPoint joinPoint, SketchFraud ann) throws Throwable {
        return process(joinPoint, "FRAUD", ann.windowMs(), ann.key(), ann.maxEvents());
    }

    @Around("@annotation(ann)")
    public Object enforceHitter(ProceedingJoinPoint joinPoint, SketchHitter ann) throws Throwable {
        return process(joinPoint, "HITTER", ann.windowMs(), ann.key(), ann.threshold());
    }

    @Around("@annotation(ann)")
    public Object enforceSurge(ProceedingJoinPoint joinPoint, SketchSurge ann) throws Throwable {
        return process(joinPoint, "SURGE", ann.windowMs(), ann.key(), ann.maxErrors());
    }

    @Around("@annotation(ann)")
    public Object enforceCheat(ProceedingJoinPoint joinPoint, SketchCheat ann) throws Throwable {
        return process(joinPoint, "CHEAT", ann.windowMs(), ann.key(), ann.maxActions());
    }

    @Around("@annotation(ann)")
    public Object enforceSensor(ProceedingJoinPoint joinPoint, SketchSensor ann) throws Throwable {
        return process(joinPoint, "SENSOR", ann.windowMs(), ann.key(), ann.maxAlerts());
    }

    private Object process(ProceedingJoinPoint joinPoint, String prefix, long windowMs, String spelKey, long maxLimit) throws Throwable {
        String sketchId = prefix + "_" + windowMs;
        ClTdsSketch sketch = sketchPool.computeIfAbsent(sketchId, id -> new ClTdsSketch(windowMs));

        long hashKey = resolveKeyHash(joinPoint, spelKey);

        sketch.increment(hashKey);
        int currentCount = sketch.query(hashKey);
        
        // Micrometer Metrics: Record the query
        meterRegistry.counter("cltds.query.count", "sketch", prefix).increment();

        if (currentCount > maxLimit) {
            // Micrometer Metrics: Record the breach
            meterRegistry.counter("cltds.threshold.breach", "sketch", prefix).increment();
            throw new SketchThresholdException(
                String.format("[%s] Limit exceeded! Key Hash: %d | Current: %d | Max Allowed: %d", 
                    prefix, hashKey, currentCount, maxLimit)
            );
        }

        return joinPoint.proceed();
    }

    private long resolveKeyHash(ProceedingJoinPoint joinPoint, String spelKey) {
        // Safe fallback hash based on the method signature
        long fallbackHash = (long) joinPoint.getSignature().toShortString().hashCode();

        if (spelKey == null || spelKey.trim().isEmpty()) {
            return fallbackHash;
        }

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            // Protect against null args array
            Object[] args = joinPoint.getArgs() != null ? joinPoint.getArgs() : new Object[0];
            
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(),
                    signature.getMethod(),
                    args,
                    nameDiscoverer
            );

            Object resolvedValue = parser.parseExpression(spelKey).getValue(context);
            if (resolvedValue == null) {
                // If the SpEL evaluates to null, don't return 0. Return a unique null-state hash
                return (long) (joinPoint.getSignature().toShortString() + "_NULL").hashCode();
            }
            
            return (long) resolvedValue.hashCode();
        } catch (Exception e) {
            // Catch SpEL Evaluation Exceptions, Parse Exceptions, or NPEs safely.
            // We should NOT crash the user's business logic just because rate limiting failed to parse a key.
            return fallbackHash;
        }
    }
}
