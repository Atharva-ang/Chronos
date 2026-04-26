# Chronos Job Scheduler - API Documentation

## Overview
Chronos is a distributed job scheduling system that supports one-time jobs, scheduled jobs, and recurring jobs with automatic retry and failure notification mechanisms.

## Table of Contents
- [Authentication](#authentication)
- [Job Management](#job-management)
- [Worker Management](#worker-management)
- [Job Types](#job-types)
- [Recurring Jobs](#recurring-jobs)
- [Scheduled Jobs](#scheduled-jobs)

---

## Authentication

### Register User
**POST** `/api/auth/register`

Register a new user account.

**Request Body:**
```json
{
  "username": "user@example.com",
  "password": "securePassword123"
}
```

**Response:**
```
User successfully registered
```

---

### Login
**POST** `/api/auth/login`

Authenticate and receive a JWT token.

**Request Body:**
```json
{
  "username": "user@example.com",
  "password": "securePassword123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Note:** Include this token in the `Authorization` header for all subsequent requests:
```
Authorization: Bearer <token>
```

---

## Job Management

### Submit a Job
**POST** `/api/jobs`

Submit a new job for execution.

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body (Immediate Execution):**
```json
{
  "jobType": "SEND_EMAIL",
  "payload": "{\"to\":\"recipient@example.com\",\"subject\":\"Test\",\"body\":\"Hello\"}"
}
```

**Request Body (Scheduled Job):**
```json
{
  "jobType": "DATA_SYNC",
  "payload": "{\"source\":\"db1\",\"target\":\"db2\"}",
  "scheduledAt": "2026-04-20T15:30:00"
}
```

**Request Body (Recurring Job):**
```json
{
  "jobType": "SEND_EMAIL",
  "payload": "{\"to\":\"admin@example.com\",\"subject\":\"Daily Report\"}",
  "isRecurring": true,
  "cronExpression": "0 9 * * *"
}
```

**Cron Expression Examples:**
- `0 9 * * *` - Daily at 9:00 AM
- `0 */6 * * *` - Every 6 hours
- `0 0 * * 0` - Weekly on Sunday at midnight
- `0 0 1 * *` - Monthly on the 1st at midnight

**Response:**
```json
{
  "id": 123,
  "username": "user@example.com",
  "jobType": "SEND_EMAIL",
  "payload": "{\"to\":\"recipient@example.com\"}",
  "status": "INQUEUE",
  "cronExpression": null,
  "isRecurring": false,
  "retryCount": 0,
  "maxRetries": 3,
  "createdAt": "2026-04-19T10:30:00",
  "scheduledAt": null,
  "lastExecutedAt": null,
  "nextExecutionAt": null
}
```

---

### Get All Jobs
**GET** `/api/jobs`

Retrieve all jobs for the authenticated user.

**Query Parameters:**
- `status` (optional): Filter by job status (`INQUEUE`, `INPROGRESS`, `COMPLETED`, `FAILED`, `CANCELLED`)

**Example:**
```
GET /api/jobs?status=COMPLETED
```

**Response:**
```json
[
  {
    "id": 123,
    "username": "user@example.com",
    "jobType": "SEND_EMAIL",
    "status": "COMPLETED",
    "createdAt": "2026-04-19T10:30:00"
  }
]
```

---

### Get Job by ID
**GET** `/api/jobs/{id}`

Retrieve details of a specific job.

**Response:**
```json
{
  "id": 123,
  "username": "user@example.com",
  "jobType": "SEND_EMAIL",
  "payload": "{\"to\":\"recipient@example.com\"}",
  "status": "COMPLETED",
  "retryCount": 0,
  "maxRetries": 3,
  "createdAt": "2026-04-19T10:30:00"
}
```

---

### Cancel Job
**PUT** `/api/jobs/{id}/cancel`

Cancel a job that is in INQUEUE status.

**Response:**
```
Job successfully cancelled.
```

**Note:** Jobs can only be cancelled if they are in `INQUEUE` status. Jobs already in progress or completed cannot be cancelled.

---

### Reschedule Job
**PUT** `/api/jobs/{id}/reschedule`

Reschedule a job with a new cron expression.

**Query Parameters:**
- `cronExpression` (required): New cron expression

**Example:**
```
PUT /api/jobs/123/reschedule?cronExpression=0 10 * * *
```

**Response:**
```json
{
  "id": 123,
  "cronExpression": "0 10 * * *",
  "status": "INQUEUE"
}
```

---

## Worker Management

### List All Jobs (Admin)
**GET** `/api/worker/jobs`

Retrieve all jobs in the system (for monitoring).

**Query Parameters:**
- `status` (optional): Filter by status

---

### Get Job Execution Logs
**GET** `/api/worker/logs`

Retrieve execution logs for jobs.

**Query Parameters:**
- `jobId` (optional): Filter logs for a specific job

**Response:**
```json
[
  {
    "id": 1,
    "jobId": 123,
    "status": "COMPLETED",
    "startedAt": "2026-04-19T10:30:00",
    "finishedAt": "2026-04-19T10:30:05",
    "errorMessage": null
  }
]
```

---

### Get System Statistics
**GET** `/api/worker/stats`

Get job statistics across all statuses.

**Response:**
```json
{
  "INQUEUE": 5,
  "INPROGRESS": 2,
  "COMPLETED": 150,
  "FAILED": 3,
  "CANCELLED": 1
}
```

---

### Requeue Job
**POST** `/api/worker/jobs/{id}/requeue`

Manually requeue a job for execution.

**Response:** `202 Accepted`

---

## Job Types

The system currently supports the following job types:

### 1. SEND_EMAIL
Simulates sending an email.

**Payload Example:**
```json
{
  "to": "recipient@example.com",
  "subject": "Test Email",
  "body": "This is a test"
}
```

### 2. DATA_SYNC
Simulates data synchronization.

**Payload Example:**
```json
{
  "source": "database1",
  "target": "database2"
}
```

---

## Recurring Jobs

Recurring jobs are automatically executed based on their cron expression.

### How It Works:
1. Submit a job with `isRecurring: true` and a valid `cronExpression`
2. The scheduler calculates the next execution time
3. The job is automatically executed at the scheduled time
4. After execution, the next execution time is recalculated
5. The job continues to execute according to the cron schedule

### Fields:
- `cronExpression`: Standard cron format (e.g., `0 9 * * *`)
- `lastExecutedAt`: Timestamp of last execution
- `nextExecutionAt`: Timestamp of next scheduled execution

### Example:
```json
{
  "jobType": "DATA_SYNC",
  "payload": "{\"source\":\"db1\",\"target\":\"db2\"}",
  "isRecurring": true,
  "cronExpression": "0 */2 * * *"
}
```
This creates a job that runs every 2 hours.

---

## Scheduled Jobs

Schedule a job to run at a specific future time.

### Example:
```json
{
  "jobType": "SEND_EMAIL",
  "payload": "{\"to\":\"user@example.com\"}",
  "scheduledAt": "2026-04-20T15:30:00"
}
```

The job will be executed at the specified time.

---

## Failure Handling

### Automatic Retry
- Failed jobs are automatically retried up to `maxRetries` times (default: 3)
- Each retry attempt is logged in the execution logs
- After max retries are exhausted, the job status is set to `FAILED`

### Notifications
When a job fails permanently:
- A notification is logged
- An email notification is sent to the user (if email is configured)
- The failure is recorded in the execution logs

### Configuring Email Notifications
To enable email notifications, add the following to `application.properties`:

```properties
# Enable notifications
chronos.notification.enabled=true
chronos.notification.from-email=noreply@chronos.com

# SMTP Configuration (uncomment and configure)
# spring.mail.host=smtp.gmail.com
# spring.mail.port=587
# spring.mail.username=your-email@gmail.com
# spring.mail.password=your-app-password
# spring.mail.properties.mail.smtp.auth=true
# spring.mail.properties.mail.smtp.starttls.enable=true
```

Also add the mail dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

---

## Job Status Flow

```
INQUEUE → INPROGRESS → COMPLETED
    ↓                      ↑
CANCELLED              FAILED (with retry)
                          ↓
                       FAILED (permanent)
```

- **INQUEUE**: Job is waiting to be executed
- **INPROGRESS**: Job is currently executing
- **COMPLETED**: Job finished successfully
- **FAILED**: Job failed after exhausting all retries
- **CANCELLED**: Job was cancelled by user

---

## Error Codes

- `400 Bad Request`: Invalid request body or parameters
- `401 Unauthorized`: Missing or invalid JWT token
- `403 Forbidden`: User doesn't have access to the resource
- `404 Not Found`: Job not found or not owned by user
- `500 Internal Server Error`: Server error during processing

---

## Rate Limits and Performance

### Scheduler Intervals:
- **Scheduled Jobs Polling**: Every 30 seconds
- **Recurring Jobs Polling**: Every 60 seconds

### Scalability:
- Worker engine can be horizontally scaled
- RabbitMQ handles job distribution across multiple workers
- Database supports concurrent job creation and updates

---

## Example Usage Flow

### 1. Register and Login
```bash
# Register
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"pass123"}'

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"pass123"}'
```

### 2. Submit a Recurring Job
```bash
curl -X POST http://localhost:8082/api/jobs \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "jobType":"SEND_EMAIL",
    "payload":"{\"to\":\"admin@example.com\"}",
    "isRecurring":true,
    "cronExpression":"0 9 * * *"
  }'
```

### 3. Monitor Jobs
```bash
# Get all jobs
curl -X GET http://localhost:8082/api/jobs \
  -H "Authorization: Bearer <token>"

# Get execution logs
curl -X GET http://localhost:8083/api/worker/logs?jobId=123
```

---

## Notes
- All timestamps are in ISO-8601 format
- Cron expressions use standard cron syntax
- JWT tokens expire after a configured time period
- Jobs are persisted in the database
- Execution logs are maintained for audit purposes
