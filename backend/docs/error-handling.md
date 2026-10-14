# Error handling

All failures leave the API in one shape:

```json
{ "success": false, "status": 409, "message": "This code already exists.", "requestId": "4aeb3ab4-..." }
```

`data` is present only for validation errors, where it maps each field to its message. `requestId` is
also in the `X-Request-Id` header and on every log line for that request, so a customer's screenshot
leads straight to the logs.

## Where failures are turned into responses

| Where the failure happens | Who answers |
| --- | --- |
| Controller, service, repository (inside Spring MVC) | `exception/GlobalExceptionHandler` |
| Spring Security (not signed in, wrong role on a URL rule) | entry point / access-denied handler in `SecurityConfig` → `ApiErrorWriter` |
| Servlet filters (rate limit) | the filter → `ApiErrorWriter` |
| Anything that still reaches `/error` | `controller/ApiErrorController` |

## Which exception to throw

| Situation | Throw | Status |
| --- | --- | --- |
| Input is invalid beyond what `@Valid` checks (unknown size, bad code, product rules) | `BadRequestException` | 400 |
| An id / slug / order number resolves to nothing | `ResourceNotFoundException` | 404 |
| Valid request, but the current state forbids it (duplicate, illegal status move, last admin) | `ConflictException` | 409 |
| Wrong password at sign-in | `UnauthorizedException` | 401 |
| Signed in but may not touch this record | `ForbiddenException` | 403 |
| Sign-in lockout | `TooManyRequestsException` | 429 |
| Required setup is missing (no store settings row) | `ServiceUnavailableException` | 503 |
| A partner (Razorpay, S3) failed | `ExternalServiceException(message, cause)` | 502 |

The message of these exceptions is shown to the user, so write it for them ("This code already exists."),
never with ids, class names or SQL.

Framework failures are mapped for you: `@Valid` → 400 with fields, unreadable JSON → 400, wrong type in a
path/param → 400, missing param/header/part → 400, wrong Content-Type → 415, wrong verb → 405, unknown
route → 404, upload too large → 413, `@PreAuthorize` denial → 401 (signed out) / 403, optimistic lock →
409, constraint violation → 409. Anything else is a bug: logged with its stack trace, answered with a
generic 500.

## Logging

- Client mistakes (4xx): one WARN line, no stack trace.
- 5xx: one ERROR with the stack trace, written by the handler. Do not also `log.error` before throwing;
  pass the cause to `ExternalServiceException` instead.

## Review checklist

- [ ] No `ex.getMessage()` of a framework or library exception returned to the client.
- [ ] No new `@RestControllerAdvice`.
- [ ] New failure paths use one of the exceptions above, with a test asserting the status.
