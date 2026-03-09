# Sistema de Riego a Goteo - API

Spring Boot REST API for an agricultural drip irrigation management system.

## Build & Run

```bash
./mvnw clean install        # compile + test
./mvnw spring-boot:run      # start server (port 8080)
./mvnw test                 # run tests only
./mvnw test jacoco:report   # run tests + generate coverage report
```

Swagger UI: http://localhost:8080/swagger-ui/index.html

## Tech Stack

- **Java 17**, Spring Boot 3.4.10
- **DB:** MySQL 8 (prod), H2 in-memory (tests)
- **Security:** Spring Security + JWT (jjwt 0.11.5)
- **ORM:** Spring Data JPA / Hibernate
- **Lombok** 1.18.28, **springdoc-openapi** 2.2.0
- Reports: OpenPDF, Apache POI, OpenCSV
- Data seeding: DataFaker 2.0.2
- Push notifications: Firebase Cloud Messaging (FCM)

## Project Structure

```
src/main/java/.../
├── config/          # SecurityConfig, JWT filter, DataInitializer, SystemConfigSeeder
├── controller/      # REST controllers (admin, analytics, audit, auth, config,
│                    #   dashboard, notification, report, riego, sync, user, weather)
├── dto/             # Request/Response DTOs per domain
├── model/           # JPA entities: riego/, user/, audit/, notification/, report/
├── repository/      # Spring Data repositories per domain
├── service/         # Business logic per domain
├── scheduler/       # WeatherScheduler
├── event/           # Domain events (HumidityAlert, Maintenance, Task)
├── exceptions/      # GlobalExceptionHandler, ResourceNotFoundException
└── util/            # GenerateSecretKey, PasswordEncoderUtil, ReportBrandingHelper
```

## Key Domain Entities (model/riego/)

Farm, Sector, Irrigation, IrrigationEquipment, HumiditySensor, HumidityAlert,
EnergyConsumption, Fertilization, WaterSource, ReservoirTurn, Precipitation,
OperationLog, Task, TaskStatus, Maintenance, UnitOfMeasure

## Configuration Notes

- `application.properties` is encoded **ISO-8859-1** (Spanish chars/tildes) — the `maven-resources-plugin` is configured accordingly in `pom.xml`
- Test profile uses `application-test.properties` with H2 + `NON_KEYWORDS=USER` (H2 reserved word workaround)
- External APIs configured in `application.properties`: OpenWeatherMap, OpenCageData geocoding, Firebase FCM

## Testing Conventions

| Annotation | Use case |
|---|---|
| `@WebMvcTest` | Controller slice tests — mock `JwtService` + `UserDetailsService` with `@MockitoBean` |
| `@DataJpaTest` | Repository/JPA tests — use `@AutoConfigureTestDatabase(Replace.NONE)` + `@ActiveProfiles("test")` |
| `@SpringBootTest` | Full integration tests (JWT security) |

**Important JPA test rules:**
- Use only `em.*` operations (not mixed with `repository.saveAndFlush`) in cascade tests
- Initialize lazy collections before `em.remove()` for `CascadeType.REMOVE` to work
- `SecurityConfig` has no `AuthenticationEntryPoint` → unauthenticated requests return **403** (not 401)

**Pre-existing test failures** (not caused by new code):
- `NotificationServiceTest.createNotification_datosValidos_guardaNotificacion`
- `FertilizationServiceTest.createFertilization_Success`
- `IrrigationServiceTest.createIrrigation_calculosCorrectos_creaIrrigacion`

## JaCoCo Coverage

Report generated at `target/site/jacoco/`. Thresholds: **70% line / 60% branch**.
