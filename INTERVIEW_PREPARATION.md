# Interview Preparation (based on this codebase)

1. **How are short codes generated and why are collisions handled twice?** 7 chars of Base62 from `SecureRandom` (62^7 ≈ 3.5 trillion). `existsByCode` skips known collisions cheaply; the unique DB constraint plus a retry on `DataIntegrityViolationException` handles concurrent inserts. Max 5 attempts, then 500.
2. **Why is `create` not `@Transactional`?** A failed INSERT aborts the Postgres transaction; any further statement in it fails. Each attempt must run in its own transaction for the retry to work.
3. **Why 302 and not 301?** Browsers cache 301s permanently, so later clicks would never hit the server and couldn't be counted.
4. **How do you count clicks without losing updates?** One atomic `UPDATE ... SET click_count = click_count + 1`. Read-modify-write in Java loses counts when two requests read the same value. A 20-thread test verifies it.
5. **Why `clearAutomatically` on the update query?** Bulk JPQL updates bypass the persistence context; clearing prevents stale entities in the same transaction.
6. **Why Flyway plus `ddl-auto=validate`?** Versioned, reviewable schema changes; Hibernate only checks the entity matches. Never edit an applied migration; add a new one.
7. **How do you stop `/swagger-ui.html` or `/api/...` matching the redirect route?** The route is restricted by regex to exactly 7 alphanumerics, so other paths don't match it.
8. **How is validation done and where?** Bean Validation on the request record (`@NotBlank`, `@Size`, `@Pattern`); failures raise `MethodArgumentNotValidException`, mapped to a 400 ProblemDetail with a field error list.
9. **What is `ProblemDetail`?** Spring's RFC 9457 type (`application/problem+json`): status, title, detail, instance, plus custom properties like `errors`.
10. **Why does the 500 response hide the exception message?** Avoid leaking internals; the exception is logged server-side.
11. **How are the tests structured?** Mockito unit tests for the service, `@WebMvcTest` slices for controllers, `@DataJpaTest` with real Postgres, `@SpringBootTest` integration tests. One shared static Testcontainers Postgres and identical annotations so the Spring context is reused.
12. **Why `open-in-view=false`?** Prevents the JPA session from staying open during view rendering and hides lazy-loading/connection-holding problems.
13. **Why is the Docker build multi-stage?** JDK and Maven only in the build stage; the runtime image is a JRE running as a non-root user. `dependency:go-offline` before copying `src` caches dependencies.
14. **How does compose ensure the app starts after the DB?** `depends_on` with `condition: service_healthy` on the postgres `pg_isready` healthcheck.
15. **How would you add rate limiting or expiry?** See [PROGRESS.md](PROGRESS.md): a filter with a token bucket returning 429; an `expires_at` column via a new migration and a check in `resolveRedirect`.
