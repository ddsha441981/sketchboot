# Getting Started

This guide will walk you through integrating Sketchboot **v1.0.1** into your Spring Boot application.

## Prerequisites

Before using Sketchboot, ensure your system meets the following requirements:
- **Java Version:** JDK 22 or higher (Sketchboot uses the Java 22 FFM API).
- **Spring Boot:** Version 3.2.x or 3.3.x.
- **Build Tool:** Maven or Gradle.

## 1. Add the Dependency

Sketchboot is published on Maven Central. Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.ddsha441981</groupId>
    <artifactId>sketchboot-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

For Gradle (`build.gradle`):
```groovy
implementation 'io.github.ddsha441981:sketchboot-spring-boot-starter:1.0.1'
```

<div style="background-color: #ffe6e6; border-left: 6px solid #ff4d4d; padding: 15px; margin-top: 15px; margin-bottom: 20px; color: #cc0000; border-radius: 4px;">
    <strong>⚠️ CRITICAL WARNING: Do not use v1.0.0 in Production</strong><br><br>
    If you are migrating or upgrading, please note that <b>v1.0.0</b> contains a critical performance bug. SpEL expressions (like <code>#userId</code>) were being dynamically parsed on <i>every single HTTP request</i>. Under high traffic, this causes massive CPU spikes and Garbage Collection (GC) overhead, completely destroying the "Zero GC" benefits of the native Rust engine.<br><br>
    <b>Always use v1.0.1 or higher</b>. In v1.0.1, the SpEL ASTs are safely cached in memory, guaranteeing absolute zero-GC overhead and true lock-free execution.
</div>

## 2. Enable Native Access

Because Sketchboot calls a native Rust library via FFM, you **must** tell the JVM to allow native access. If you skip this step, Java 22 will throw an `IllegalCallerException`.

Add this JVM argument when running your application:
```bash
--enable-native-access=ALL-UNNAMED
```

### In Maven (pom.xml)
To run via `mvn spring-boot:run`, configure the Spring Boot plugin:
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>--enable-native-access=ALL-UNNAMED</jvmArguments>
    </configuration>
</plugin>
```

### In IntelliJ IDEA / Eclipse
Go to your Run/Debug Configurations -> "Modify options" -> "Add VM options" and paste:
`--enable-native-access=ALL-UNNAMED`

## 3. Protect Your First Endpoint

Simply add the `@SketchLimit` annotation to any REST controller method.

```java
import io.github.ddsha441981.sketchboot.annotation.SketchLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

    // Allow 100 requests per 60 seconds.
    // The `#userId` dynamically reads the method parameter.
    @SketchLimit(requests = 100, windowMs = 60000, key = "#userId")
    @GetMapping("/api/data")
    public String getData(String userId) {
        return "Data for " + userId;
    }
}
```

That's it! Your endpoint is now secured by a blazing-fast native rate limiter.
