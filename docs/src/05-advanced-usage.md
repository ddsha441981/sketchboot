# Advanced Usage & Configuration

## Exception Handling

When a rate limit is breached, Sketchboot throws a `SketchThresholdException`. 
Sketchboot includes a built-in Spring `@ControllerAdvice` called `SketchExceptionHandler` which automatically catches this exception and returns a **429 Too Many Requests** HTTP response.

You can override this by defining your own `@ExceptionHandler` in your application:

```java
@RestControllerAdvice
public class CustomRateLimitHandler {

    @ExceptionHandler(SketchThresholdException.class)
    public ResponseEntity<Map<String, Object>> handleLimit(SketchThresholdException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of(
                "error", "Custom Rate Limit Exceeded",
                "reason", ex.getMessage()
            ));
    }
}
```

## Micrometer Metrics Integration

Sketchboot automatically integrates with `Micrometer`. It publishes two key metrics that you can view in Prometheus/Grafana or the `/actuator/metrics` endpoint:

1. `cltds.query.count`: A counter of total requests evaluated by Sketchboot.
2. `cltds.threshold.breach`: A counter of how many requests were blocked.

Both metrics have a `sketch` tag representing the annotation type (e.g., `LIMIT`, `FRAUD`, `SHIELD`), so you can build beautiful Grafana dashboards showing traffic distribution by protection layer.

## Dynamic Keys with SpEL

The power of the `key` parameter lies in SpEL.

**By IP Address:**
If your method accepts `HttpServletRequest`:
```java
@SketchLimit(requests = 10, key = "#request.remoteAddr")
public void endpoint(HttpServletRequest request) {}
```

**By Complex Object:**
If you pass a complex object (like a JWT Token or DTO):
```java
@SketchLimit(requests = 5, key = "#payload.user.email")
public void endpoint(@RequestBody UserPayload payload) {}
```

If the SpEL expression cannot be parsed or evaluates to `null`, Sketchboot gracefully falls back to a global method-level hash, ensuring your application never crashes due to a rate-limiting configuration error.
