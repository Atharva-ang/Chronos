# Chronos — Distributed Job Scheduling System

A robust, scalable, microservices-based job scheduling system built with Spring Boot 4, RabbitMQ, and PostgreSQL. Chronos supports one-time jobs, scheduled jobs, recurring cron-based jobs, automatic retries with exponential backoff, and execution logging.

---

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   auth-service  │     │    job-api       │     │  worker-engine  │
│   Port: 8081    │     │   Port: 8082     │     │   Port: 8083    │
│                 │     │                 │     │                 │
│  JWT Auth       │     │  Job CRUD        │     │  Job Executor   │
│  Register/Login │     │  Scheduler       │     │  RabbitMQ       │
└─────────────────┘     └────────┬────────┘     │  Listener       │
                                 │               └────────┬────────┘
                                 │                        │
                         ┌───────▼────────┐              │
                         │   RabbitMQ     │◄─────────────┘
                         │   Port: 5672   │
                         └───────┬────────┘
                                 │
                         ┌───────▼────────┐
                         │  PostgreSQL    │
                         │  Port: 5432   │
                         └───────────────┘
```

### Services
- **auth-service** (`:8081`) — User registration and JWT-based authentication
- **job-api** (`:8082`) — Job submission, management, and scheduling
- **worker-engine** (`:8083`) — Job execution, monitoring, and logging

---

## Getting Started

### Prerequisites
- Docker Desktop
- Git

### Run the Project

```bash
# Clone the repository
git clone <your-repo-url>
cd Chronos

# Start all services
docker-compose up --build
```

All services will be available once you see:
```
Started JobApiChronosApplication
Started AuthServiceChronosApplication
Started WorkerEngineChronosApplication
```

---

## API Documentation

### Authentication (Port 8081)

#### Register
```
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "pass123",
  "email": "user@example.com"
}
```
Response: `User successfully registered`

#### Login
```
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "pass123"
}
```
Response:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

> Use this token in all subsequent requests as `Authorization: Bearer <token>`

---

### Job Management (Port 8082)

#### Submit Immediate Job
```
POST http://localhost:8082/api/jobs
Authorization: Bearer <token>

{
  "jobType": "SEND_EMAIL",
  "payload": "{\"to\":\"user@example.com\",\"subject\":\"Hello\"}"
}
```

#### Submit Scheduled Job
```
POST http://localhost:8082/api/jobs
Authorization: Bearer <token>

{
  "jobType": "DATA_SYNC",
  "payload": "{\"source\":\"db1\",\"target\":\"db2\"}",
  "scheduledAt": "2026-05-01T10:00:00"
}
```

#### Submit Recurring Job
```
POST http://localhost:8082/api/jobs
Authorization: Bearer <token>

{
  "jobType": "SEND_EMAIL",
  "payload": "{\"to\":\"admin@example.com\",\"subject\":\"Daily Report\"}",
  "isRecurring": true,
  "cronExpression": "0 9 * * *"
}
```

#### Get All Jobs
```
GET http://localhost:8082/api/jobs
GET http://localhost:8082/api/jobs?status=COMPLETED
Authorization: Bearer <token>
```

#### Get Job by ID
```
GET http://localhost:8082/api/jobs/{id}
Authorization: Bearer <token>
```

#### Cancel Job
```
PUT http://localhost:8082/api/jobs/{id}/cancel
Authorization: Bearer <token>
```

#### Reschedule Job
```
PUT http://localhost:8082/api/jobs/{id}/reschedule?cronExpression=0 10 * * *
Authorization: Bearer <token>
```

---

### Worker / Monitoring (Port 8083)

#### List All Jobs
```
GET http://localhost:8083/api/worker/jobs
GET http://localhost:8083/api/worker/jobs?status=FAILED
Authorization: Bearer <token>
```

#### Get Execution Logs
```
GET http://localhost:8083/api/worker/logs
GET http://localhost:8083/api/worker/logs?jobId=1
Authorization: Bearer <token>
```

#### Get System Stats
```
GET http://localhost:8083/api/worker/stats
Authorization: Bearer <token>
```
Response:
```json
{
  "INQUEUE": 2,
  "INPROGRESS": 1,
  "COMPLETED": 15,
  "FAILED": 0,
  "CANCELLED": 1
}
```

#### Requeue a Job
```
POST http://localhost:8083/api/worker/jobs/{id}/requeue
Authorization: Bearer <token>
```
Response: `202 Accepted`

---

## Job Types

| Type | Description | Payload Fields |
|------|-------------|----------------|
| `SEND_EMAIL` | Simulates sending an email | `to`, `subject`, `body` |
| `DATA_SYNC` | Simulates data synchronization | `source`, `target` |

---

## Job Status Flow

```
INQUEUE → INPROGRESS → COMPLETED
   ↓                        ↑
CANCELLED               FAILED (retried)
                            ↓
                        FAILED (permanent)
```

---

## Cron Expression Examples

| Expression | Meaning |
|------------|---------|
| `0 9 * * *` | Every day at 9:00 AM |
| `0 */6 * * *` | Every 6 hours |
| `0 0 * * 0` | Every Sunday at midnight |
| `0 0 1 * *` | First of every month |

---

## Failure Handling

- Failed jobs are automatically retried up to 3 times
- Each retry uses **exponential backoff** (2s → 4s → 8s) to avoid overloading the system
- After max retries, the job is marked `FAILED` and a notification is logged
- All failures are recorded in execution logs with error messages

---

## Design Decisions

### Why Microservices?
Separating auth, job management, and worker execution allows each service to scale independently. Under heavy load, multiple worker instances can be deployed without touching auth or the API layer.

### Why RabbitMQ?
RabbitMQ decouples job submission from execution. The job-api publishes jobs to a queue and returns immediately, while the worker processes them asynchronously — making the API fast and non-blocking.

### Why Exponential Backoff?
Fixed retry delays cause CPU spikes when many jobs fail simultaneously. Exponential backoff spreads retries out over time, preventing the worker from hammering the system.

### Why PostgreSQL?
Jobs and logs need to persist reliably across restarts. PostgreSQL provides ACID guarantees and supports the complex queries needed for scheduled and recurring job lookups.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres-db:5432/chronos_db` | DB connection |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
| `SPRING_RABBITMQ_HOST` | `rabbitmq` | RabbitMQ host |

---

## Scheduler Intervals

- Scheduled jobs are polled every **30 seconds**
- Recurring jobs are polled every **60 seconds**

---

## Notes

- JWT tokens expire after 24 hours — re-login to get a new token
- All timestamps are in ISO-8601 format
- Cron expressions follow standard Unix cron syntax
- The RabbitMQ management UI is available at `http://localhost:15672` (guest/guest)
- TRYNNA TRY NEW THINGS. DOING THIS FOR PR.