# Annotations Reference

Sketchboot provides **7 distinct annotations** to handle various security and rate-limiting use cases. All annotations use the native 1MB sketch backend.

### Universal Parameter: `key`
All annotations accept a `key` parameter. This is a **Spring Expression Language (SpEL)** string.
- By default, if omitted, the key is the hash of the target method's signature. This applies a global rate limit to the endpoint.
- You can use `#parameterName` to limit by a method argument.
- You can use `#request.remoteAddr` to limit by IP if `HttpServletRequest` is a parameter.

---

## 1. `@SketchLimit`
General purpose API rate limiting.

- **Parameters:**
  - `requests` (long): Maximum allowed requests.
  - `windowMs` (long): Time window in milliseconds (default: 60000).
- **Use Case:** Preventing users from spamming an API endpoint (e.g., max 100 API calls per minute).

```java
@SketchLimit(requests = 100, windowMs = 60000, key = "#userId")
public void userDashboard(String userId) { ... }
```

---

## 2. `@SketchShield`
Defense against intense volumetric attacks (DDoS protection).

- **Parameters:**
  - `threshold` (long): Maximum allowed hits.
  - `windowMs` (long): Time window in milliseconds (default: 60000).
- **Use Case:** Blocking IP addresses that are attempting to flood a specific public endpoint.

```java
@SketchShield(threshold = 5000, windowMs = 10000, key = "#ipAddress")
public void publicPage(String ipAddress) { ... }
```

---

## 3. `@SketchFraud`
Anti-fraud detection for sensitive events.

- **Parameters:**
  - `maxEvents` (long): Maximum allowed events.
  - `windowMs` (long): Time window in milliseconds (default: 60000).
- **Use Case:** Limiting login attempts, OTP verifications, or credit card validation to prevent brute-force or credential stuffing.

```java
@SketchFraud(maxEvents = 5, windowMs = 300000, key = "#email")
public void attemptLogin(String email, String password) { ... }
```

---

## 4. `@SketchSurge`
Handling unexpected spikes or errors.

- **Parameters:**
  - `maxErrors` (long): Maximum allowed surges/errors.
  - `windowMs` (long): Time window in milliseconds (default: 60000).
- **Use Case:** Circuit breaking logic. If an endpoint causes more than `X` errors/retries from a specific client, block further attempts.

```java
@SketchSurge(maxErrors = 50, windowMs = 10000, key = "#tenantId")
public void heavyDatabaseQuery(String tenantId) { ... }
```

---

## 5. `@SketchCheat`
Gaming and high-frequency action limitation.

- **Parameters:**
  - `maxActions` (long): Maximum allowed rapid actions.
  - `windowMs` (long): Time window in milliseconds (default: 60000).
- **Use Case:** In gaming or trading platforms, preventing users from firing too many actions (like clicking a button 50 times a second using a macro).

```java
@SketchCheat(maxActions = 10, windowMs = 1000, key = "#playerId")
public void fireWeapon(String playerId) { ... }
```

---

## 6. `@SketchHitter`
Heavy-hitter detection (Analytics).

- **Parameters:**
  - `threshold` (long): Threshold to be considered a heavy hitter.
  - `windowMs` (long): Time window in milliseconds (default: 60000).
- **Use Case:** Used to identify users or IPs that consume a disproportionate amount of bandwidth or resources.

```java
@SketchHitter(threshold = 1000, windowMs = 3600000, key = "#apiKey")
public void downloadLargeFile(String apiKey) { ... }
```

---

## 7. `@SketchSensor`
Security Operations Center (SOC) alerting.

- **Parameters:**
  - `maxAlerts` (long): Maximum allowed suspicious activities.
  - `windowMs` (long): Time window in milliseconds (default: 60000).
- **Use Case:** Monitoring scraping bots or anomalous behavior across endpoints.

```java
@SketchSensor(maxAlerts = 20, windowMs = 60000, key = "#botIp")
public void scrapeData(String botIp) { ... }
```
