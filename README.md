# SmartCampus — Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey)** for managing campus rooms, IoT sensors, and sensor readings as part of the university's SmartCampus initiative.

---

## Table of Contents
- [API Design Overview](#api-design-overview)
- [Build & Run Instructions](#build--run-instructions)
- [Sample curl Commands](#sample-curl-commands)
- [Report: Conceptual Questions & Answers](#report-conceptual-questions--answers)
  - [Part 1: Service Architecture & Setup](#part-1-service-architecture--setup)
  - [Part 2: Room Management](#part-2-room-management)
  - [Part 3: Sensor Operations & Linking](#part-3-sensor-operations--linking)
  - [Part 4: Deep Nesting with Sub-Resources](#part-4-deep-nesting-with-sub-resources)
  - [Part 5: Advanced Error Handling & Logging](#part-5-advanced-error-handling--logging)

---

## API Design Overview

The SmartCampus API follows a **resource-oriented architecture** aligned with RESTful principles. It manages three core entities:

| Resource | Base Path | Description |
|----------|-----------|-------------|
| **Room** | `/api/v1/rooms` | Physical rooms on campus |
| **Sensor** | `/api/v1/sensors` | IoT devices deployed in rooms |
| **SensorReading** | `/api/v1/sensors/{id}/readings` | Historical measurement data |

### Architecture Highlights

- **JAX-RS (Jersey)** with Grizzly embedded HTTP server
- **In-memory data storage** using `ConcurrentHashMap` and `ArrayList`
- **Sub-Resource Locator Pattern** for sensor readings (clean delegation)
- **Custom Exception Mappers** for HTTP 409, 422, 403, and 500
- **Request/Response Logging Filter** for API observability
- **HATEOAS-style Discovery Endpoint** at `GET /api/v1`

### Resource Hierarchy

```
/api/v1                          → API Discovery & Metadata
/api/v1/rooms                    → Room Collection (GET, POST)
/api/v1/rooms/{roomId}           → Individual Room (GET, DELETE)
/api/v1/sensors                  → Sensor Collection (GET, POST)
/api/v1/sensors?type=CO2         → Filtered Sensor Collection
/api/v1/sensors/{sensorId}       → Individual Sensor (GET)
/api/v1/sensors/{sensorId}/readings → Sensor Reading History (GET, POST)
```

---

## Build & Run Instructions

### Prerequisites
- **Java 11** or later (JDK)
- **Apache Maven 3.6+**

### Step 1: Clone the Repository
```bash
git clone https://github.com/<your-username>/smartcampus-api.git
cd smartcampus-api
```

### Step 2: Build the Project
```bash
mvn clean compile
```

### Step 3: Run the Server
```bash
mvn exec:java
```

The server will start on **http://localhost:8080**. You should see:
```
SmartCampus API is RUNNING
Base URI : http://localhost:8080/
API Root : http://localhost:8080/api/v1
```

### Alternative: Build and Run as a Fat JAR
```bash
mvn clean package
java -jar target/smartcampus-api-1.0-SNAPSHOT.jar
```

### Step 4: Test the API
Open a new terminal and use the curl commands below to interact with the API.

---

## Sample curl Commands

### 1. Discovery Endpoint — API Metadata
```bash
curl -s http://localhost:8080/api/v1 | python3 -m json.tool
```

### 2. Create a New Room
```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "CS-401", "name": "Computer Science Lab", "capacity": 40}' \
  | python3 -m json.tool
```

### 3. List All Rooms
```bash
curl -s http://localhost:8080/api/v1/rooms | python3 -m json.tool
```

### 4. Register a New Sensor (Linked to an Existing Room)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "LIGHT-001", "type": "Lighting", "status": "ACTIVE", "currentValue": 750.0, "roomId": "CS-401"}' \
  | python3 -m json.tool
```

### 5. Post a Sensor Reading and Verify Side Effect
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 23.7}' \
  | python3 -m json.tool
```

### 6. Filter Sensors by Type
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | python3 -m json.tool
```

### 7. Attempt to Delete a Room with Active Sensors (→ 409 Conflict)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

### 8. Attempt to Register a Sensor with Invalid Room ID (→ 422)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "ERR-001", "type": "Temperature", "status": "ACTIVE", "currentValue": 0, "roomId": "NONEXISTENT"}' \
  | python3 -m json.tool
```

### 9. Post a Reading to a Sensor in MAINTENANCE Mode (→ 403 Forbidden)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 19.5}' \
  | python3 -m json.tool
```

---

## Report: Conceptual Questions & Answers

### Part 1: Service Architecture & Setup

#### Question 1.1: JAX-RS Resource Class Lifecycle

The default lifecycle of a JAX-RS resource class is **request-scoped** (also known as per-request). This means the JAX-RS runtime (e.g., Jersey) creates a **new instance** of each resource class for every incoming HTTP request. Once the request is processed and the response is sent, the instance is eligible for garbage collection.

**Impact on In-Memory Data Structures:**

Because each request gets its own resource instance, any data stored as instance variables within a resource class would be lost after each request. This is why we must use a **shared, external data store** — in our implementation, a thread-safe singleton `DataStore` class that uses `ConcurrentHashMap`.

To prevent data loss and race conditions:
- We use `ConcurrentHashMap` instead of `HashMap` because multiple request threads may read/write concurrently
- The `DataStore` is a singleton accessed via a static `getInstance()` method, ensuring all resource instances share the same data
- `ConcurrentHashMap` provides atomic operations like `putIfAbsent()` and `computeIfAbsent()` to prevent race conditions without explicit synchronisation

If we had used `@Singleton` on the resource class instead, a single instance would serve all requests. While this reduces object creation overhead, it introduces concurrency risks because multiple threads could simultaneously modify instance fields, leading to data corruption if not properly synchronised.

#### Question 1.2: HATEOAS and Hypermedia in RESTful Design

**HATEOAS** (Hypermedia as the Engine of Application State) is considered a hallmark of advanced RESTful design because it makes APIs **self-documenting and navigable**. Instead of requiring clients to hardcode every endpoint URL from static documentation, the API itself provides links to related resources and available actions within each response.

**Benefits over static documentation:**

1. **Discoverability**: Clients can navigate the entire API by following links from the root endpoint, similar to how a user navigates a website by clicking links. Our discovery endpoint at `GET /api/v1` provides a map of all primary resource collections.

2. **Decoupling**: Client implementations become loosely coupled to specific URL structures. If the server changes a URL path (e.g., from `/api/v1/rooms` to `/api/v2/rooms`), HATEOAS-driven clients adapt automatically by following updated links rather than breaking.

3. **Reduced Errors**: Static documentation can become outdated. Hypermedia links are always in sync with the server because they are generated at runtime.

4. **Guided Workflows**: The server can conditionally include or exclude links based on the current resource state, effectively guiding the client through valid state transitions (e.g., only showing a "delete" link when deletion is permitted).

5. **Self-Contained Responses**: Each response contains everything the client needs to take the next step, eliminating the need to cross-reference external documentation.

---

### Part 2: Room Management

#### Question 2.1: Returning IDs vs Full Objects

When returning a list of rooms, there are significant trade-offs between returning only resource IDs versus returning the full room objects:

**Returning Full Objects (our implementation):**
- **Pros**: Reduces the number of HTTP round-trips. The client receives all data in a single request, which is more efficient for client-side rendering and processing. This is especially beneficial for mobile clients where network latency is high.
- **Cons**: Larger response payloads consume more bandwidth. If the client only needs a subset of the data (e.g., just room names), the extra fields represent wasted bandwidth.

**Returning IDs Only:**
- **Pros**: Minimal payload size. The client can selectively fetch only the rooms it needs, which is efficient when dealing with very large collections where the client typically only needs a few items.
- **Cons**: Leads to the **N+1 problem** — the client must make one request for the list, then N additional requests to fetch each room's details. This dramatically increases network overhead and latency, especially over slow connections.

For the SmartCampus API, returning full objects is the better choice because campus management applications typically need to display complete room information in dashboards, and the total number of rooms is manageable enough that payload size is not a concern.

#### Question 2.2: Idempotency of DELETE

**Yes, the DELETE operation is idempotent in our implementation.**

Idempotency means that making the same request multiple times produces the same server-side effect as making it once. Here is what happens when a client sends the same DELETE request multiple times:

1. **First call**: The room exists and has no sensors. The server removes the room from the data store and returns **HTTP 204 No Content**. Server state change: room is deleted.

2. **Second call (and beyond)**: The room no longer exists. The server looks up the room, finds nothing, and returns **HTTP 404 Not Found**. Server state: unchanged (room is still absent).

The key insight is that while the **HTTP status codes differ** (204 vs 404), the **server-side state** after any number of identical calls is identical: the room does not exist. The purpose of idempotency is to guarantee that retrying a request (e.g., due to network timeout) does not cause unintended side effects like deleting a different room or corrupting data. Our implementation satisfies this guarantee.

This is consistent with the HTTP specification (RFC 7231), which states that the server state after N>0 identical requests should be the same as after a single request.

---

### Part 3: Sensor Operations & Linking

#### Question 3.1: @Consumes and Content-Type Mismatches

When we annotate a POST method with `@Consumes(MediaType.APPLICATION_JSON)`, we explicitly declare that this endpoint only accepts `application/json` request bodies.

**Technical consequences of sending the wrong Content-Type:**

If a client sends data with a `Content-Type` of `text/plain` or `application/xml`, the JAX-RS runtime will **reject the request before it even reaches our resource method**. The runtime performs content negotiation by comparing the request's `Content-Type` header against the `@Consumes` annotation:

1. **Mismatch detected**: JAX-RS finds no matching `MessageBodyReader` capable of deserialising the incoming media type into the expected Java object.

2. **HTTP 415 Unsupported Media Type**: The runtime automatically returns this status code to the client, indicating that the server refuses to accept the request because the payload format is not supported by the target resource for the requested method.

3. **No custom error handling needed**: This validation happens at the framework level, before any business logic executes. The resource method is never invoked.

This is a powerful feature of JAX-RS because it provides automatic input validation at the protocol level, ensuring that our business logic only receives properly formatted JSON data. It also follows the principle of "fail fast" — the client is informed immediately about the format mismatch rather than receiving a confusing deserialization error.

#### Question 3.2: @QueryParam vs Path Parameter for Filtering

Our implementation uses `@QueryParam("type")` for filtering sensors (e.g., `GET /sensors?type=CO2`). An alternative design would embed the type in the URL path (e.g., `GET /sensors/type/CO2`).

**Why query parameters are superior for filtering and searching collections:**

1. **Optionality**: Query parameters are inherently optional. `GET /sensors` returns all sensors, while `GET /sensors?type=CO2` returns only CO2 sensors. With path-based design, you would need two separate endpoints (`/sensors` and `/sensors/type/{type}`), increasing code duplication and routing complexity.

2. **Composability**: Query parameters can be combined freely. For example, `GET /sensors?type=CO2&status=ACTIVE` allows multi-criteria filtering without complex path structures. Path-based designs would require increasingly awkward nested paths like `/sensors/type/CO2/status/ACTIVE`.

3. **Resource Identity**: In RESTful design, the URL path identifies a **resource**. `/sensors` identifies the sensor collection — it's the same collection whether filtered or not. Filtering criteria should not change the resource identity; they are modifiers on how the collection is presented. Query parameters correctly express this semantic distinction.

4. **Cacheability**: Proxies and CDNs can cache responses based on the full URL including query strings. Different filter combinations produce different cache entries naturally.

5. **Convention**: The HTTP specification and industry standards (e.g., Google, GitHub, Twitter APIs) universally use query parameters for filtering, sorting, and pagination. Using path parameters for these purposes would violate developer expectations and make the API harder to use.

---

### Part 4: Deep Nesting with Sub-Resources

#### Question 4.1: Architectural Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern is a powerful architectural tool in JAX-RS that allows a parent resource class to **delegate** handling of nested paths to a dedicated child resource class.

In our implementation, `SensorResource` delegates `{sensorId}/readings` to `SensorReadingResource`:

```java
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

**Architectural benefits:**

1. **Separation of Concerns**: Each resource class is responsible for exactly one logical entity. `SensorResource` handles sensor CRUD, while `SensorReadingResource` handles reading history. Neither class needs to know the internal implementation details of the other.

2. **Reduced Complexity**: Without sub-resource locators, a single `SensorResource` class would need to contain methods for `GET /sensors`, `POST /sensors`, `GET /sensors/{id}`, `GET /sensors/{id}/readings`, `POST /sensors/{id}/readings`, and potentially `GET /sensors/{id}/readings/{readingId}`. This creates a monolithic "god class" that is difficult to read, test, and maintain.

3. **Improved Testability**: Smaller, focused classes can be unit-tested independently. `SensorReadingResource` can be tested in isolation by simply constructing it with a sensor ID, without setting up the full sensor routing infrastructure.

4. **Team Scalability**: In larger projects, different team members can work on different sub-resource classes simultaneously without merge conflicts or stepping on each other's code.

5. **Reusability**: Sub-resource classes can potentially be reused. For example, if readings needed to be accessible from another path, the same `SensorReadingResource` class could be instantiated from a different parent locator.

6. **Encapsulation of Context**: The locator method captures the parent context (the `sensorId`) and passes it to the sub-resource, establishing a clear parent-child relationship. This ensures the sub-resource always operates in the correct context.

---

### Part 5: Advanced Error Handling & Logging

#### Question 5.2: Why HTTP 422 Over 404 for Missing References

HTTP **422 Unprocessable Entity** is more semantically accurate than HTTP **404 Not Found** when the issue is a missing reference inside a valid JSON payload.

**The key distinction:**

- **404 Not Found** means the **target resource** (identified by the URL) does not exist. For example, `GET /sensors/NONEXISTENT` returning 404 correctly means "there is no sensor at this URL."

- **422 Unprocessable Entity** means the server received the request, the URL target **does exist** (`POST /sensors` is a valid endpoint), and the JSON payload is **syntactically valid** — but the server **cannot process it** due to semantic errors in the data.

When a client sends `POST /sensors` with `{"roomId": "NONEXISTENT"}`:
- The URL `/sensors` exists and accepts POST requests ✓
- The JSON body is well-formed and parseable ✓
- But the `roomId` value references a room that doesn't exist ✗

Using 404 here would be misleading because it would imply the `/sensors` endpoint itself doesn't exist. The 422 status code correctly communicates: "I understood your request and the syntax is fine, but I can't process it because the data contains invalid references."

This distinction helps client developers quickly identify whether the issue is with the URL they're calling (404) or with the data they're sending (422).

#### Question 5.4: Cybersecurity Risks of Exposing Stack Traces

Exposing internal Java stack traces to external API consumers poses significant cybersecurity risks:

1. **Technology Stack Disclosure**: Stack traces reveal the programming language (Java), framework (JAX-RS/Jersey), and specific library versions. Attackers can use this to search for known CVEs (Common Vulnerabilities and Exposures) targeting those exact versions.

2. **Internal Architecture Exposure**: Package names (e.g., `com.smartcampus.resource.SensorResource`) reveal the application's internal class structure, naming conventions, and architectural patterns. This provides a roadmap for understanding how the application is organised.

3. **File System Path Leakage**: Stack traces often include absolute file paths (e.g., `/home/deploy/app/src/main/java/...`), revealing the server's operating system, deployment structure, and potentially user account names.

4. **Business Logic Insights**: Method names and call chains in the stack trace reveal business logic flow, error handling patterns, and potential weak points where input validation might be lacking.

5. **SQL/Query Exposure**: If database-related exceptions bubble up, the stack trace might contain partial SQL queries, table names, or connection strings — directly enabling SQL injection attacks.

6. **Dependency Mapping**: Third-party library classes appearing in the trace (e.g., specific Jackson or Hibernate versions) allow attackers to identify and exploit known vulnerabilities in those dependencies.

Our `GenericExceptionMapper` implementation mitigates all these risks by intercepting all unhandled `Throwable` instances, logging the full details server-side for debugging, and returning only a sanitized, generic error message to the client.

#### Question 5.5: Advantages of JAX-RS Filters for Logging

Using JAX-RS filters for cross-cutting concerns like logging is significantly advantageous over manually inserting `Logger.info()` statements inside every resource method:

1. **DRY Principle (Don't Repeat Yourself)**: The logging logic is defined once in the filter class, not duplicated across dozens of resource methods. Changing the log format or adding new fields requires editing a single file.

2. **Consistency**: Every request and response is logged uniformly. With manual logging, developers might forget to add logging to new endpoints, use inconsistent formats, or log at different severity levels.

3. **Separation of Concerns**: Resource methods focus exclusively on business logic. Logging, authentication, CORS, compression, and other cross-cutting concerns are handled externally by filters, keeping the codebase clean and modular.

4. **Automatic Coverage**: The filter applies to **all** endpoints automatically, including any new resources added in the future. Manual logging requires explicit effort for each new endpoint.

5. **Lifecycle Awareness**: Filters have access to both the request and response contexts, allowing them to capture the complete request-response lifecycle (method, URI, headers, status code, timing) in a way that would require complex boilerplate if done manually.

6. **Configurability**: Filters can be enabled/disabled, prioritised (using `@Priority`), or scoped to specific resources using `@NameBinding` annotations, providing fine-grained control without modifying business logic.
# smartcampus-api
# smartcampus-api
