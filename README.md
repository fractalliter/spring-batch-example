# Winner selection batch pipeline

In this project you can find a demonstration of a [batch processing](https://en.wikipedia.org/wiki/Batch_processing)
pipeline
with [Spring Batch Framework](https://docs.spring.io/spring-batch/docs/current/reference/html/spring-batch-intro.html)
which integrates with [Apache Kafka](https://kafka.apache.org/) and [PostgreSQL](https://www.postgresql.org/).

## How to run

You need [Docker Compose](https://docs.docker.com/compose/) installed. Copy
`.env.example` to `.env` and set a strong `DB_PASSWORD`. The `.env` file is
ignored by Git and excluded from the Docker build context. From the project root:

```bash
cp -n .env.example .env
# Edit DB_PASSWORD in .env before starting.
docker compose up
```

This starts PostgreSQL, Kafka (KRaft mode), and the application. The application
uses Java 21 and the `prod` profile. The batch job does not run on startup; its
scheduler runs every Sunday at 23:59. The first start initializes the database
schema. PostgreSQL data is kept in the `pgdata` Docker volume. The Spring Batch
tables are installed by PostgreSQL when this volume is first created; an older
volume without those tables must be migrated before starting the app.

For local development, start only the dependencies, export the values from
`.env`, then run the app with Java 21:

```bash
docker compose up -d postgres kafka
set -a; source .env; set +a
PROFILE=dev ./mvnw spring-boot:run
```

`DB_HOST_PORT` and `KAFKA_EXTERNAL_HOST_PORT` are the ports used by the local
app. Internal container ports are configured separately in `.env`.

## How to run tests

The tests use H2 and embedded Kafka; no Docker services are required.

```bash
./mvnw test
```

## Job -> LuckyWinnerJob

This job is selecting a weekly winner from a list of customers who meet the threshold amount for transactions.

### Scheduler

Scheduler executes a cron job right at the end of each week and runs
the [selectingWeeklyWinner](https://github.com/fractalliter/jackpot-winner/blob/f8d1db2e8aa75fad3fb7c0054b158b265525113f/src/main/java/com/ecom/winners/configurations/BatchConfiguration.java#L28)
job.

```Java
@Scheduled(cron = "59 23 * * * SUN") // executes every Sunday at 23:59
```

### Steps

There are three steps for selecting a lucky winner:

1. [Fetching data](https://github.com/fractalliter/jackpot-winner/blob/f8d1db2e8aa75fad3fb7c0054b158b265525113f/src/main/java/com/ecom/winners/configurations/UserData.java#L62)
   of users from an API with the help of WebClients
2. [Streaming the transactions](https://github.com/fractalliter/jackpot-winner/blob/f8d1db2e8aa75fad3fb7c0054b158b265525113f/src/main/java/com/ecom/winners/configurations/UserTransaction.java#L55)
   for users from a CSV file
3.
Randomly [selecting a user](https://github.com/fractalliter/jackpot-winner/blob/f8d1db2e8aa75fad3fb7c0054b158b265525113f/src/main/java/com/ecom/winners/configurations/SelectWinner.java#L65)
from the database

Each step has three phases:

1. Reader
2. Processor
3. Writer

### Step 1.Fetching User's Data

In this step, user data will be fetched from an API, data transformed into entity and persisted into the database.

##### 1.1 Reader

Reader starts with reading user data from an API by sending a `GET` request. The request uses WebClient library for
Spring WebFlux to fetch data from the API, it uses a builder approach and can be configured for different use cases.
You can configure the page size and the starting page alongside a `Predicate<Integer>` for finishing the read.
Data will be mapped to a DTO class.

##### 1.2 Processor

The Processor is dedicated to mapping the DTO to database entity. It uses a `Function<UserDTO, USER>` to map UserDTO
into User entity.

##### 1.3 Writer

The Writer takes to User entity from the processor and uses entity manager to persist it int users table. If the user
already exits and the data has changed, it will utilize a merge mechanism.

#### Step 2.User transactions from CSV

In this step user's transactions are streamed from
a [CSV file](https://github.com/fractalliter/jackpot-winner/blob/main/src/main/resources/data/transactions.csv) with the
help of `FlatFileItemReader<TransactionDTO>` as
transactions dto. The transaction DTO passes over to processor where it will check the `user_id` from transactions
against users in the database with help of `UserRepository` find by user ID method and passes the Transaction entity
over to JPA writer to persist it into the database as `transactions` table.

##### 2.1 Reader

Reader starts with streaming the rows from csv file by the help of `FlatFileItemReader` that can be built with multiple
parameters from properties like a resource path and headers. The rows from CSV maps to `TransactionDTO`.

##### 2.2 Processor

Processor checks the `user_id` from **TransactionDTO** with help of **UserRepository** and finds the equivalent user in
the database then creates the Transaction entity based on the user and amount. Transaction entity generates a
**creation date** for each Transaction.

##### 2.3 Writer

Writer takes **Transaction** entity from processor, persists into the database with `JpaItemWriter` which manages
all the required relation between user and it's transactions.

#### Step 3.Selection of Winner

The selection of the winner acts like the punchline of the selecting lucky winner. It executes a summation query in
the database to select the users who have specific criteria, _purchase amount more than the threshold_. The lucky winner
will be selected and mapped into a DTO. In the end, it will be published into a Kafka topic for further processing.

##### 3.1 Reader

The Reader executes a summation SQL query against user's transactions which lays within the last week and picks the ones
who meet the threshold amount criteria. Then It randomly orders them and applies another random OFFSET to selected users
and takes
one row who is the lucky winner.

##### 3.2 Processor

The Processor maps the User entity into a UserDTO and passes it over to the Writer.

##### 3.3 Writer

The Writer takes the UserDTO from the processor and publish it into a Kafka topic for further processing. the message
key is generated from hashcode of the winner. The message value is a JSON of UserDTO.

>For more please read the **[documents](https://fractalliter.github.io/spring-batch-example/index.html)**

## Dashboard và API

Sao chép `.env.example` thành `.env`, đặt mật khẩu PostgreSQL, rồi chạy backend và hạ tầng bằng `docker compose up --build`. Backend chỉ mở trên `127.0.0.1:${APP_HOST_PORT}`; cổng trong container là `APP_PORT`. Lịch chạy mặc định là Chủ Nhật 23:59 theo `APP_TIME_ZONE`.

Chạy giao diện ở máy host:

```bash
cd frontend
pnpm install
pnpm dev
```

Mở `http://localhost:5173` (hoặc cổng `FRONTEND_PORT` trong `.env`). Vite chuyển tiếp `/api` đến backend qua `APP_HOST_PORT`. Giao diện tự cập nhật thống kê và lịch sử mỗi 5 giây, và nút **Chạy job** gọi API để chạy thủ công.

| Endpoint | Chức năng |
| --- | --- |
| `GET /api/dashboard` | Thống kê người dùng/giao dịch, cấu hình và 10 lần chạy gần nhất |
| `POST /api/jobs/executions` (`Content-Type: application/json`, body `{}`) | Chạy job; trả về `executionId` |

Metadata của Spring Batch được lưu trong các bảng `BATCH_*` của PostgreSQL. Khi dùng volume PostgreSQL cũ chưa có các bảng này, cần tạo schema từ `docker/postgres/schema-batch.sql` trước khi khởi động app. Nút chạy và API điều khiển job chỉ được mở trên localhost trong cấu hình Compose mặc định.
