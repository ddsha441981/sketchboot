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

package io.github.ddsha441981.sketchboot.config;

import io.github.ddsha441981.sketchboot.actuator.SketchEndpoint;
import io.github.ddsha441981.sketchboot.aop.SketchAspect;
import io.github.ddsha441981.sketchboot.exception.SketchExceptionHandler;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot AutoConfiguration for Sketchboot.
 * This class automatically provisions the necessary AOP aspects, exception handlers,
 * and Actuator endpoints when the starter is included in a Spring Boot project.
 */
@AutoConfiguration
public class SketchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SketchAspect sketchAspect(MeterRegistry meterRegistry) {
        return new SketchAspect(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.ControllerAdvice")
    public SketchExceptionHandler sketchExceptionHandler() {
        return new SketchExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    public SketchEndpoint sketchEndpoint(SketchAspect sketchAspect) {
        return new SketchEndpoint(sketchAspect);
    }
}
