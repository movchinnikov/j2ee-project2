# 🧹 Cleaning Platform — Microservice Application

> **J2EE Course Project | Humber College | Semester 4**
>
> A production-grade cleaning services booking platform built with Spring Boot microservices, JWT authentication, Docker Compose orchestration, and a Thymeleaf UI gateway.

---

## 👥 Team

| Name | Role |
|---|---|
| **Maksim Ovchinnikov** | Team Lead / Backend Engineer |
| **Harpreet Kaur** | Frontend Developer |
| **Pragati Sahani** | QA / Tester |

---

## 📐 Architecture Overview

The platform consists of **5 independent Spring Boot services**, each with its **own dedicated PostgreSQL database** running on a **shared PostgreSQL instance** (microservice-per-database pattern).

```
┌─────────────────────────────────────────────────────────┐
│                   Browser / Postman                      │
└──────────────────────┬──────────────────────────────────┘
                       │
          ┌────────────▼────────────┐
          │ Booking/UI Service      │  :18090 → booking_db
          │ (Thymeleaf + REST proxy)│
          └───┬────────────────────┘
              │
 ┌────────────▼────────┐
 │   IAC Service       │  :18080 → iac_db
 │   (auth/users/roles)│
 └─────────────────────┘
              │ HTTP
 ┌────────────▼──────────────────────┐
 │ Property │ Pricing  │ Order       │
 │ :18081   │ :18082   │ :18083      │
 │ prop_db  │ price_db │ order_db    │
 └──────────┴──────────┴─────────────┘
              │
 ┌────────────▼────────────────────────────────────────┐
 │          PostgreSQL :15432 (single instance)         │
 │  iac_db │ property_db │ pricing_db │ order_db │ booking_db │
 └───────────────────────────────────────────────────────┘
```

### Database Isolation

Each service owns its database, has a dedicated DB user, and manages its schema via Flyway:

| Service | Database | User | Schema |
|---|---|---|---|
| IAC Service | `iac_db` | `iac_user` | `public` |
| Property Service | `property_db` | `property_user` | `property` |
| Pricing Service | `pricing_db` | `pricing_user` | `pricing` |
| Order Service | `order_db` | `order_user` | `orders` |
| Booking/UI Service | `booking_db` | `booking_user` | `booking` |

> **Cross-service references** (e.g., `client_id` in `property_db` referencing `iac_db.users`) are stored as plain UUIDs — no DB-level foreign keys across databases. Consistency is maintained at the application level.

### Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| **IAC Service** | `18080` | `iac_db` | Identity & Access Control — registration, login, JWT tokens, role management |
| **Property Service** | `18081` | `property_db` | Client property management (apartments, houses, offices) |
| **Pricing Service** | `18082` | `pricing_db` | Service catalog (types + addons) and price calculation engine |
| **Order Service** | `18083` | `order_db` | Order lifecycle, round-robin cleaner assignment, earnings tracking |
| **Booking/UI Service** | `18090` | `booking_db` | Frontend (Thymeleaf) + integrated booking facade + reverse proxy |
| **PostgreSQL** | `15432` | — | Shared DB instance hosting all 5 databases |

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop ≥ 24
- Java 17+ (for local development only)
- Postman (for API testing)

### Run with Docker Compose

```bash
# Clone the repo
git clone https://github.com/<your-org>/j2ee-project2.git
cd j2ee-project2

# Build and start all services
docker compose up --build

# Or in background
docker compose up --build -d
```

Wait ~30 seconds for all services to start. Then:

| URL | Description |
|---|---|
| `http://localhost:18090` | Web UI |
| `http://localhost:18080/swagger-ui.html` | IAC Service Swagger |
| `http://localhost:18081/swagger-ui.html` | Property Service Swagger |
| `http://localhost:18082/swagger-ui.html` | Pricing Service Swagger |
| `http://localhost:18083/swagger-ui.html` | Order Service Swagger |

### Default Super Admin Credentials
```
Username: admin
Password: password
```
> ⚠️ Change these in `docker-compose.yml` for any real deployment.

---

## 🔐 Authentication

The platform uses **JWT Bearer tokens** with automatic rotation.

### Token Lifecycle

```
POST /api/v1/auth/login
  → accessToken  (15 minutes)
  → refreshToken (7 days, single-use with rotation)

POST /api/v1/auth/refresh?refreshToken=<token>
  → new accessToken + new refreshToken

POST /api/v1/auth/logout
  → all refresh tokens revoked
```

### Roles

| Role | Permissions |
|---|---|
| `ROLE_USER` | Default role after registration |
| `ROLE_CLIENT` | Create properties, place + pay orders |
| `ROLE_CLEANER` | View schedule, complete orders, track earnings |
| `ROLE_ADMIN` | Manage users and roles |
| `ROLE_SUPER_ADMIN` | All admin rights + cannot be revoked |

---

## 📬 Postman Collection

A comprehensive Postman collection is included in the `postman/` directory.

### Collections

| File | Description |
|---|---|
| `postman/CleaningPlatform_Full.postman_collection.json` | **Full collection** — all 5 services, all endpoints |
| `postman/IAC_Service.postman_collection.json` | IAC Service only (legacy) |

### How to Import

1. Open Postman → **Import**
2. Select `postman/CleaningPlatform_Full.postman_collection.json`
3. Go to **Variables** tab and verify base URLs
4. Run **"Login as Super Admin"** — tokens are auto-saved
5. All other requests inherit `{{accessToken}}` automatically

### Collection Variables

| Variable | Default Value | Description |
|---|---|---|
| `iacUrl` | `http://localhost:18080` | IAC Service |
| `propertyUrl` | `http://localhost:18081` | Property Service |
| `pricingUrl` | `http://localhost:18082` | Pricing Service |
| `orderUrl` | `http://localhost:18083` | Order Service |
| `bookingUrl` | `http://localhost:18090` | Booking / UI Service |
| `accessToken` | *(auto-set)* | JWT access token |
| `refreshToken` | *(auto-set)* | JWT refresh token |
| `propertyId` | *(auto-set)* | Last created property UUID |
| `orderId` | *(auto-set)* | Last created order UUID |

### Token Auto-Save Script

Every login/register/refresh request runs this Post-Response Script automatically:

```javascript
var json = pm.response.json();
if (json.data && json.data.accessToken) {
  pm.collectionVariables.set('accessToken',  json.data.accessToken);
  pm.collectionVariables.set('refreshToken', json.data.refreshToken);
}
```

---

## 🗺️ API Reference

### IAC Service (`:18080`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | None | Register new user |
| POST | `/api/v1/auth/login` | None | Login, get tokens |
| POST | `/api/v1/auth/refresh` | None | Refresh access token |
| POST | `/api/v1/auth/logout` | Bearer | Revoke refresh tokens |
| GET | `/api/v1/users/me` | Bearer | Get own profile |
| GET | `/api/v1/users` | ADMIN+ | List all users (paginated) |
| GET | `/api/v1/users/{id}` | ADMIN+ | Get user by ID |
| DELETE | `/api/v1/users/{id}` | SUPER_ADMIN | Delete user |
| GET | `/api/v1/admin/roles` | ADMIN+ | List available roles |
| POST | `/api/v1/admin/assign-role` | ADMIN+ | Assign role to user |
| POST | `/api/v1/admin/revoke-role` | ADMIN+ | Revoke role from user |

### Property Service (`:18081`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/properties` | CLIENT | Add property |
| GET | `/api/v1/properties` | Any | List my properties |
| GET | `/api/v1/properties/{id}` | Any | Get property by ID |
| PUT | `/api/v1/properties/{id}` | CLIENT | Update property |
| DELETE | `/api/v1/properties/{id}` | CLIENT | Soft-delete property |
| GET | `/api/v1/properties/{id}/validate` | Any | [Internal] Validate ownership |

### Pricing Service (`:18082`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/catalog/services` | None | List service types |
| GET | `/api/v1/catalog/addons` | None | List service addons |
| POST | `/api/v1/pricing/calculate` | None | Calculate price |

**Price Calculation Request:**
```json
{
  "areaSqm": 75.5,
  "bathroomsCount": 1,
  "serviceTypeCode": "STANDARD",
  "addonCodes": ["OVEN", "FRIDGE"],
  "frequency": "ONE_TIME"
}
```

**Frequencies:** `ONE_TIME`, `WEEKLY`, `BI_WEEKLY`, `MONTHLY`

**Service Types:** `STANDARD`, `DEEP`, `MOVE_IN_OUT`

### Order Service (`:18083`)

**Client endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/orders` | CLIENT | Create order (PENDING) |
| POST | `/api/v1/orders/{id}/pay` | CLIENT | Pay → CONFIRMED + assign cleaner |
| GET | `/api/v1/orders` | Any | List my orders |
| GET | `/api/v1/orders/{id}` | Any | Get order details |
| DELETE | `/api/v1/orders/{id}` | CLIENT | Cancel order |

**Cleaner endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/cleaner/schedule` | CLEANER | View upcoming jobs |
| PUT | `/api/v1/cleaner/profile/availability` | CLEANER | Toggle availability |
| POST | `/api/v1/cleaner/orders/{id}/complete` | CLEANER | Mark as completed |
| POST | `/api/v1/cleaner/orders/{id}/request-replacement` | CLEANER | Request replacement |
| GET | `/api/v1/cleaner/earnings` | CLEANER | Total earnings |
| GET | `/api/v1/cleaner/earnings/{year}/{month}` | CLEANER | Monthly earnings |

### Order Status Flow

```
PENDING ──[pay]──▶ CONFIRMED ──[complete]──▶ COMPLETED
    │                   │
    └──[cancel]──▶ CANCELLED
```

---

## 📊 Diagrams

All diagrams are generated with PlantUML (B&W) and located in `docs/diagrams/`:

| File | Type | Description |
|---|---|---|
| `01_component_diagram.png` | Component | Full system architecture |
| `02_sequence_login.png` | Sequence | Login and JWT token flow |
| `03_sequence_order_flow.png` | Sequence | Complete order lifecycle |
| `04_class_diagram.png` | Class | Domain entity model |

Regenerate diagrams:
```bash
java -jar /tmp/plantuml.jar -tpng docs/diagrams/*.puml
```

---

## 🐳 Docker Compose Services

```yaml
# Port mapping:
postgres         : 15432:5432   # shared PostgreSQL instance
iac-service      : 18080:8080   # iac_db
property-service : 18081:8080   # property_db
pricing-service  : 18082:8080   # pricing_db
order-service    : 18083:8080   # order_db
ui-service       : 18090:8090   # booking_db
```

### Database Provisioning

On **first startup**, PostgreSQL runs `docker/postgres/init.sql` which:
1. Ensures `iac_db` / `iac_user` exists (POSTGRES_DB default)
2. Creates `property_db` + `property_user`
3. Creates `pricing_db` + `pricing_user`
4. Creates `order_db` + `order_user`
5. Creates `booking_db` + `booking_user`

Each service then runs **Flyway** on its own database at startup, applying its migrations under its schema.

> ⚠️ If you had a previous installation with all data in `iac_db`, tear down and recreate the volume:
> ```bash
> docker compose down -v
> docker compose up --build
> ```

### Useful commands

```bash
# View logs for a specific service
docker compose logs -f iac-service

# Restart a single service (after code changes)
docker compose up --build iac-service -d

# Stop everything and remove volumes
docker compose down -v

# Check health
docker compose ps
```

---

## 🏗️ Project Structure

```
j2ee-project2/
├── docker-compose.yml          # Orchestration config
├── run.sh                      # Helper startup script
├── .gitignore
├── README.md
│
├── iac-service/                # Identity & Access Control
│   ├── src/main/java/com/project/iac/
│   │   ├── web/controller/     # AuthController, UserController, AdminController
│   │   ├── service/            # AuthService, UserService, RoleService
│   │   ├── security/           # JwtAuthFilter, JwtTokenProvider, SecurityConfig
│   │   ├── domain/entity/      # UserEntity, RoleEntity, RefreshTokenEntity
│   │   └── repository/
│   └── Dockerfile
│
├── property-service/           # Client property management
│   ├── src/main/java/com/project/property/
│   │   ├── web/controller/     # PropertyController
│   │   ├── domain/entity/      # PropertyEntity
│   │   └── repository/
│   └── Dockerfile
│
├── pricing-service/            # Service catalog and pricing engine
│   ├── src/main/java/com/project/pricing/
│   │   ├── web/controller/     # PricingController
│   │   ├── service/            # PricingEngine
│   │   ├── domain/entity/      # ServiceTypeEntity, ServiceAddonEntity
│   │   └── repository/
│   └── Dockerfile
│
├── order-service/              # Order lifecycle and cleaner management
│   ├── src/main/java/com/project/order/
│   │   ├── web/controller/     # ClientOrderController, CleanerController
│   │   ├── service/            # OrderService, RoundRobinAssigner
│   │   ├── domain/entity/      # OrderEntity, CleanerProfileEntity, EarningEntity
│   │   └── repository/
│   └── Dockerfile
│
├── booking-service/            # Integrated booking facade
│   ├── src/main/java/com/project/booking/
│   │   ├── web/controller/     # ClientController, CleanerController, ServiceCatalogController
│   │   ├── service/            # OrderService, EarningService
│   │   ├── domain/entity/      # OrderEntity, PropertyEntity, ...
│   │   └── repository/
│   └── Dockerfile
│
├── ui-service/                 # Frontend + reverse proxy
│   ├── src/main/java/com/project/ui/
│   │   └── controller/         # HomeController, ProxyController
│   └── Dockerfile
│
├── postman/
│   ├── CleaningPlatform_Full.postman_collection.json
│   └── IAC_Service.postman_collection.json
│
└── docs/
    └── diagrams/
        ├── 01_component_diagram.puml / .png
        ├── 02_sequence_login.puml / .png
        ├── 03_sequence_order_flow.puml / .png
        └── 04_class_diagram.puml / .png
```

---

## ⚙️ Environment Variables

Key environment variables (configured in `docker-compose.yml`):

| Variable | Service | Description |
|---|---|---|
| `JWT_SECRET` | All | Shared JWT signing secret |
| `JWT_ACCESS_TOKEN_EXPIRY_MS` | IAC | Access token TTL (default: 900000 = 15 min) |
| `JWT_REFRESH_TOKEN_EXPIRY_MS` | IAC | Refresh token TTL (default: 604800000 = 7 days) |
| `APP_SUPERADMIN_USERNAME` | IAC | Super admin username |
| `APP_SUPERADMIN_PASSWORD` | IAC | Super admin password |
| `SERVICES_PRICING_URL` | Order | Internal URL of pricing-service |
| `SERVICES_PROPERTY_URL` | Order | Internal URL of property-service |

---

## 🧪 Testing

### Postman Tests

The collection includes automated Postman tests for all endpoints. Run the full collection using Newman:

```bash
npm install -g newman

newman run postman/CleaningPlatform_Full.postman_collection.json \
  --env-var "iacUrl=http://localhost:18080" \
  --env-var "propertyUrl=http://localhost:18081" \
  --env-var "pricingUrl=http://localhost:18082" \
  --env-var "orderUrl=http://localhost:18083" \
  --env-var "bookingUrl=http://localhost:18090" \
  --reporters cli,json \
  --reporter-json-export results.json
```

### Recommended Test Workflow

1. **Register** a new user → tokens auto-saved
2. **Assign CLIENT role** (via Super Admin)
3. **Add a property** → propertyId auto-saved
4. **Calculate price** for the property
5. **Create order** → orderId auto-saved
6. **Pay order** → cleaner assigned via Round-Robin
7. **Login as Cleaner** → view schedule → complete order
8. **Check earnings**

---

## 📄 License

MIT License — Humber College J2EE Course Project 2026
