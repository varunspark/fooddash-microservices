# 🍴 FoodDash Microservices

The same food delivery idea as the monolith, split into **small independent services**.
Each service has one job, its own database, its own Dockerfile and its own pipeline step,
and one `docker compose up` starts the whole system.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-informational)

---

## Table of contents

1. [Architecture](#architecture)
2. [The services](#the-services)
3. [Project structure](#project-structure)
4. [Run it](#run-it)
5. [API reference](#api-reference)
6. [Try these experiments](#try-these-experiments)
7. [Monolith versus microservices](#monolith-versus-microservices)
8. [CI pipeline](#ci-pipeline)
9. [What is deliberately left out](#what-is-deliberately-left-out)
10. [Roadmap](#roadmap)

---

## Architecture

```
                     ┌────────────────────────────────────────────────────────────┐
                     │                    Docker network                          │
                     │                                                            │
 Browser ──:3000──>  │  frontend ──> api-gateway ──┬──> restaurant-service ──> restaurant-db
                     │  (nginx)       (:8080)      │        (:8081)            (PostgreSQL)
                     │                             │              ^
                     │                             └──> order-service ──> order-db
                     │                                    (:8082)       (PostgreSQL)
                     │                                       │
                     │                                       └── calls restaurant-service directly
                     └────────────────────────────────────────────────────────────┘
```

**One request, end to end** (placing an order):

1. The browser sends `POST /api/orders` to **nginx** on port 3000.
2. nginx forwards anything under `/api/` to the **API gateway**.
3. The gateway sees the path starts with `/api/orders` and forwards it to the **order service**.
4. The order service asks the **restaurant service** for the real price of every item (the client's prices are never trusted).
5. It saves the order in **its own database** and answers `201 Created`.
6. The answer travels back the same way to the browser.

---

## The services

| Service | Port | Job | Owns |
|---|---|---|---|
| `frontend` | 3000 | Serves the web page; forwards `/api` to the gateway | Static HTML, CSS, JS |
| `api-gateway` | 8080 | Single entry point; routes requests by path | Routing rules only |
| `restaurant-service` | 8081 | Restaurants, menus, search | `restaurant-db` |
| `order-service` | 8082 | Placing orders, order history, status | `order-db` |
| `restaurant-db` | none | PostgreSQL for restaurants | Volume `restaurant-data` |
| `order-db` | none | PostgreSQL for orders | Volume `order-data` |

**Rules this project follows**

- **Database per service.** The order service never reads the restaurant tables. It asks the restaurant service over HTTP.
- **Services find each other by name**, for example `http://restaurant-service:8081`, never `localhost`. The address comes from an environment variable.
- **The gateway hides internal endpoints.** `/api/menu-items/**` exists only for the order service and is not routed publicly.
- **The order service copies what it needs.** It stores the restaurant name and item prices on the order, so history stays correct even if the restaurant changes later.
- **Every service has a health endpoint** (`/actuator/health`) that Docker uses to decide when a service is ready.

---

## Project structure

```
fooddash-microservices/
├── docker-compose.yml
├── scripts/smoke-test.sh
├── .github/workflows/ci.yml
├── api-gateway/                 Spring Cloud Gateway
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/resources/application.yml      <- the routing table
├── restaurant-service/          Spring Boot REST API
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/...                  model, repository, web, config (seed data)
├── order-service/               Spring Boot REST API
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/...                  model, dto, client (calls restaurant-service), service, web
└── frontend/                    nginx + plain HTML/CSS/JS
    ├── Dockerfile
    ├── nginx.conf
    └── html/
```

Each service folder is a **complete, separate project**. Any of them could be moved to its own Git repository.

---

## Run it

### Prerequisites

- Docker with Docker Compose v2

### Start everything

```bash
docker compose up --build
```

The first build takes several minutes (three Maven builds). When the logs settle, open:

- **Web app:** <http://localhost:3000>
- **API (through the gateway):** <http://localhost:8080/api/restaurants>

Check the state of all containers:

```bash
docker compose ps
```

All services should show `healthy`.

### Verify with the smoke test

```bash
./scripts/smoke-test.sh
```

It checks health, lists restaurants, places a real order through the whole chain, and confirms the internal endpoint is not exposed. Each line prints `PASS` or `FAIL`.

### Stop

```bash
docker compose down        # stop and remove containers, keep the data
docker compose down -v     # also delete both database volumes
```

### Run a service without Docker (development)

Each service falls back to an in-memory H2 database, so you can run them in separate terminals:

```bash
cd restaurant-service && mvn spring-boot:run     # http://localhost:8081
cd order-service      && mvn spring-boot:run     # http://localhost:8082
cd api-gateway        && mvn spring-boot:run     # http://localhost:8080
```

Then use the API through `http://localhost:8080`. The web page itself needs the nginx container, because the plain static files cannot forward `/api` calls.

### Run the tests

```bash
cd restaurant-service && mvn test
cd order-service      && mvn test
cd api-gateway        && mvn test
```

The order service tests replace the restaurant service with a mock, so no other service has to be running.

---

## API reference

All calls go through the gateway at `http://localhost:8080`.

### Restaurants

| Method | Path | Description |
|---|---|---|
| GET | `/api/restaurants?q=` | List restaurants; `q` matches name, cuisine or dish |
| GET | `/api/restaurants/{id}` | One restaurant |
| GET | `/api/restaurants/{id}/menu` | Its menu |
| POST | `/api/restaurants` | Create a restaurant |
| POST | `/api/restaurants/{id}/menu` | Add a menu item |

### Orders

| Method | Path | Description |
|---|---|---|
| POST | `/api/orders` | Place an order |
| GET | `/api/orders?customerName=` | Orders for a customer (all orders if omitted) |
| GET | `/api/orders/{id}` | One order |
| PUT | `/api/orders/{id}/status` | Change status (an operator action) |

Statuses: `PLACED`, `PREPARING`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`. Delivered and cancelled orders cannot change again.

### Example calls

```bash
# place an order
curl -s -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerName":"Asha","deliveryAddress":"HSR Layout, Bengaluru","items":[{"menuItemId":1,"quantity":2}]}'

# move it to the next status (open the order page in the browser and watch it update)
curl -s -X PUT http://localhost:8080/api/orders/1/status \
  -H 'Content-Type: application/json' -d '{"status":"PREPARING"}'

# add a restaurant
curl -s -X POST http://localhost:8080/api/restaurants \
  -H 'Content-Type: application/json' \
  -d '{"name":"Kebab Kitchen","cuisine":"Mughlai","emoji":"🍢","rating":4.1,"deliveryTimeMins":35}'
```

Errors always look like `{"error": "..."}`.

---

## Try these experiments

These show what microservices actually change. Run them with the system up.

**1. Break one service and watch the rest survive**

```bash
docker compose stop restaurant-service
```

- The restaurant list in the browser now fails.
- **My orders still works**, because the order service has its own database.
- Placing an order returns **503** with a clear message, instead of hanging. This is the timeout in `RestaurantClient`.

```bash
docker compose start restaurant-service
```

After a few seconds, everything recovers by itself.

**2. Read the logs of one service**

```bash
docker compose logs -f order-service
```

In a monolith, everything is mixed in one log. Here you can look at one service at a time.

**3. Look at the two databases**

```bash
docker volume ls
docker compose exec order-db psql -U fooddash -d orders -c '\dt'
docker compose exec restaurant-db psql -U fooddash -d restaurants -c '\dt'
```

Two separate databases, two separate sets of tables.

**4. Rebuild only one service**

```bash
docker compose up -d --build order-service
```

The other services are not touched. This is the main operational benefit: small, independent deployments.

**5. Prove the internal endpoint is hidden**

```bash
curl -i http://localhost:8080/api/menu-items/1      # 404 through the gateway
```

---

## Monolith versus microservices

| | Monolith (`fooddash`) | Microservices (this project) |
|---|---|---|
| Deployables | 1 JAR | 3 JARs and a web server |
| Containers | 2 (app, database) | 6 |
| Database | One, shared by everything | One per service |
| A function call between parts | In-process, instant, cannot fail | Over the network, can be slow or fail |
| Deploying a small change | Rebuild and restart everything | Rebuild one service |
| One part crashes | Everything goes down | Other parts keep working |
| Local development | Very simple | More moving parts |
| Debugging | One log, one stack trace | Logs and requests spread over services |
| Consistency | Easy: one database transaction | Hard: no transaction across services |
| Best when | Small team, early product | Many teams, parts that scale or change differently |

Microservices solve organisation and scaling problems, and they create operational ones.
For a small app, the monolith is usually the better choice. This project exists so you can practise
building, connecting, running and checking several services, which is much of what a DevOps engineer does.

---

## CI pipeline

`.github/workflows/ci.yml` has two jobs:

1. **services**: builds, tests and Docker-builds each of the three Java services **in parallel** (a build matrix).
2. **integration**: starts the whole system with `docker compose up --wait`, runs `scripts/smoke-test.sh`, prints the logs if anything failed, and always cleans up.

The second job proves that the services work **together**, which no single service's tests can show.

---

## What is deliberately left out

To keep the project readable, these are not included:

- **Authentication.** Anyone can call any endpoint, including changing order status. A real system validates a JWT at the gateway.
- **Service discovery.** Docker Compose DNS is enough here. Kubernetes provides the same idea with Services.
- **Circuit breakers and retries.** There are only timeouts. Resilience4j is the usual next step.
- **Messaging.** Services talk through synchronous HTTP only. Order status changes would normally be published as events (Kafka or RabbitMQ).
- **Distributed tracing.** There is no request ID following a call across services.
- **Secrets management.** The database password defaults to a demo value. Set `DB_PASSWORD` in your environment, and use a secrets manager in production.
- **Database migrations.** Tables are created by Hibernate (`ddl-auto=update`). Use Flyway or Liquibase in production.

---

## Roadmap

- [ ] Kubernetes manifests (Deployment, Service, ConfigMap, Secret, Ingress) for every service
- [ ] Terraform for the AWS side: VPC, ECR, ECS or EKS, RDS
- [ ] Prometheus and Grafana using the Actuator metrics
- [ ] Centralised logs (Loki or the ELK stack)
- [ ] JWT authentication at the gateway and a user service
- [ ] Order events through RabbitMQ or Kafka
- [ ] Resilience4j circuit breaker around the restaurant client
- [ ] Jenkins pipeline that pushes images to a registry and deploys

---

FoodDash is an educational project. It is not affiliated with Swiggy.
