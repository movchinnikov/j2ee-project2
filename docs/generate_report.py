#!/usr/bin/env python3
"""
Generate Project Report PDF for Cleaning Platform (J2EE Project 2)
"""
import os

try:
    from reportlab.lib.pagesizes import letter
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.lib.units import inch
    from reportlab.lib import colors
    from reportlab.platypus import (
        SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
        PageBreak, HRFlowable, Image, KeepTogether
    )
    from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY
except ImportError:
    print("reportlab not installed. Run: pip3 install reportlab")
    exit(1)

# Paths
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # project root
DIAGRAMS_DIR = os.path.join(BASE_DIR, "docs", "diagrams")
OUTPUT_PDF = os.path.join(BASE_DIR, "docs", "report", "Cleaning_Platform_Report.pdf")
os.makedirs(os.path.dirname(OUTPUT_PDF), exist_ok=True)

# ─────────────────────────────────────────────────────────
#  Styles
# ─────────────────────────────────────────────────────────
styles = getSampleStyleSheet()

# Base
normal = styles['Normal']
normal.fontSize = 11
normal.leading = 16
normal.fontName = 'Helvetica'

title_style = ParagraphStyle(
    'ReportTitle',
    fontSize=26,
    fontName='Helvetica-Bold',
    alignment=TA_CENTER,
    spaceAfter=6,
    textColor=colors.black
)

subtitle_style = ParagraphStyle(
    'Subtitle',
    fontSize=14,
    fontName='Helvetica',
    alignment=TA_CENTER,
    spaceAfter=4,
    textColor=colors.HexColor('#444444')
)

h1 = ParagraphStyle(
    'H1',
    fontSize=18,
    fontName='Helvetica-Bold',
    spaceBefore=20,
    spaceAfter=10,
    textColor=colors.black,
    borderPad=4,
    borderWidth=0,
    borderColor=colors.black
)

h2 = ParagraphStyle(
    'H2',
    fontSize=14,
    fontName='Helvetica-Bold',
    spaceBefore=14,
    spaceAfter=6,
    textColor=colors.black
)

h3 = ParagraphStyle(
    'H3',
    fontSize=12,
    fontName='Helvetica-BoldOblique',
    spaceBefore=10,
    spaceAfter=4,
    textColor=colors.HexColor('#333333')
)

body = ParagraphStyle(
    'Body',
    fontSize=11,
    fontName='Helvetica',
    leading=16,
    spaceBefore=4,
    spaceAfter=4,
    alignment=TA_JUSTIFY
)

code_style = ParagraphStyle(
    'Code',
    fontSize=9,
    fontName='Courier',
    leading=13,
    spaceBefore=4,
    spaceAfter=4,
    backColor=colors.HexColor('#F5F5F5'),
    leftIndent=12,
    rightIndent=12,
    borderPad=6
)

bullet = ParagraphStyle(
    'Bullet',
    fontSize=11,
    fontName='Helvetica',
    leading=16,
    spaceBefore=2,
    spaceAfter=2,
    leftIndent=20,
    bulletIndent=10
)

# ─────────────────────────────────────────────────────────
#  Helper
# ─────────────────────────────────────────────────────────
def hr(): return HRFlowable(width="100%", thickness=1, color=colors.HexColor('#CCCCCC'), spaceAfter=10, spaceBefore=10)
def sp(h=10): return Spacer(1, h)
def P(text, style=body): return Paragraph(text, style)
def B(text): return P(f"\u2022  {text}", bullet)

def table(data, col_widths, header=True):
    t = Table(data, colWidths=col_widths)
    ts = [
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold') if header else ('FONTNAME', (0, 0), (-1, -1), 'Helvetica'),
        ('FONTSIZE', (0, 0), (-1, -1), 10),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#E0E0E0')) if header else ('BACKGROUND', (0, 0), (-1, -1), colors.white),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#F9F9F9')]) if header else ('BACKGROUND', (0, 0), (-1, -1), colors.white),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#BBBBBB')),
        ('LEFTPADDING', (0, 0), (-1, -1), 6),
        ('RIGHTPADDING', (0, 0), (-1, -1), 6),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('WORDWRAP', (0, 0), (-1, -1), True),
    ]
    t.setStyle(TableStyle(ts))
    return t

def diagram(filename, caption, max_w=6.3*inch, max_h=5.5*inch):
    path = os.path.join(DIAGRAMS_DIR, filename)
    if not os.path.exists(path):
        return [P(f"[Diagram not found: {filename}]")]
    img = Image(path)
    ratio = img.imageWidth / img.imageHeight
    # Fit within max_w x max_h maintaining aspect ratio
    w = min(max_w, img.imageWidth)
    h = w / ratio
    if h > max_h:
        h = max_h
        w = h * ratio
    img.drawWidth = w
    img.drawHeight = h
    cap_style = ParagraphStyle('Cap', fontSize=9, fontName='Helvetica-Oblique', alignment=TA_CENTER, spaceAfter=8)
    return [img, P(f"<i>Figure: {caption}</i>", cap_style)]


# ─────────────────────────────────────────────────────────
#  Document build
# ─────────────────────────────────────────────────────────
doc = SimpleDocTemplate(
    OUTPUT_PDF,
    pagesize=letter,
    leftMargin=1*inch,
    rightMargin=1*inch,
    topMargin=1*inch,
    bottomMargin=1*inch,
    title="Cleaning Platform — Project Report",
    author="Maksim Ovchinnikov, Harpreet Kaur, Pragati Sahani"
)

story = []

# ═══════════════════════════════════════════════════════════
#  TITLE PAGE
# ═══════════════════════════════════════════════════════════
story += [
    sp(100),
    P("Cleaning Platform", title_style),
    P("Microservice Web Application", subtitle_style),
    sp(20),
    hr(),
    sp(10),
    P("J2EE Enterprise Application Development", subtitle_style),
    P("Humber College — Semester 4", subtitle_style),
    sp(30),
    table(
        [
            ["Team Member", "Role"],
            ["Maksim Ovchinnikov", "Team Lead / Backend Engineer"],
            ["Harpreet Kaur", "Frontend Developer"],
            ["Pragati Sahani", "QA / Tester"],
        ],
        [3*inch, 3*inch]
    ),
    sp(40),
    P("April 2026", ParagraphStyle('date', fontSize=12, fontName='Helvetica', alignment=TA_CENTER)),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  TABLE OF CONTENTS (static)
# ═══════════════════════════════════════════════════════════
story += [
    P("Table of Contents", h1),
    hr(),
    P("1. Project Overview", body),
    P("2. Team Contributions", body),
    P("3. System Architecture", body),
    P("4. Service Descriptions", body),
    P("&nbsp;&nbsp;&nbsp;&nbsp;4.1 IAC Service (Identity & Access Control)", body),
    P("&nbsp;&nbsp;&nbsp;&nbsp;4.2 Property Service", body),
    P("&nbsp;&nbsp;&nbsp;&nbsp;4.3 Pricing Service", body),
    P("&nbsp;&nbsp;&nbsp;&nbsp;4.4 Order Service", body),
    P("&nbsp;&nbsp;&nbsp;&nbsp;4.5 Booking Service", body),
    P("&nbsp;&nbsp;&nbsp;&nbsp;4.6 UI Service", body),
    P("5. Domain Model", body),
    P("6. Messaging Architecture (Kafka + Outbox Pattern)", body),
    P("7. Authentication &amp; Security", body),
    P("8. API Reference", body),
    P("9. Sequence Diagrams", body),
    P("10. Postman Collection", body),
    P("11. Deployment", body),
    P("12. Testing", body),
    P("13. Conclusion", body),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  1. PROJECT OVERVIEW
# ═══════════════════════════════════════════════════════════
story += [
    P("1. Project Overview", h1),
    hr(),
    P("""
    The <b>Cleaning Platform</b> is a full-stack, microservice-based web application built with
    Java, Spring Boot 3, and Docker Compose. It models a real-world cleaning services marketplace
    where <b>clients</b> can register, add properties, and book cleaning orders; <b>cleaners</b>
    can view their schedule and log completions; and <b>administrators</b> can manage users and roles.
    """, body),
    sp(8),
    P("The application demonstrates the following enterprise Java concepts:", body),
    B("Microservice decomposition with clear bounded contexts per service"),
    B("JWT-based stateless authentication with refresh token rotation"),
    B("Inter-service REST communication (Order→Pricing, Order→Property)"),
    B("Spring Security with method-level RBAC (@PreAuthorize)"),
    B("Spring Data JPA with PostgreSQL and Flyway DB migrations"),
    B("Docker Compose orchestration with health checks and shared networking"),
    B("Thymeleaf-based UI gateway acting as a reverse proxy"),
    B("RESTful API design with OpenAPI (Swagger) documentation"),
    sp(10),
    P("Technology Stack", h2),
    table(
        [
            ["Layer", "Technology"],
            ["Language", "Java 17+"],
            ["Framework", "Spring Boot 3.x"],
            ["Security", "Spring Security + JWT (JJWT / custom)"],
            ["Database", "PostgreSQL 16"],
            ["ORM", "Spring Data JPA + Hibernate"],
            ["Migrations", "Flyway"],
            ["Build", "Apache Maven"],
            ["Containerization", "Docker + Docker Compose"],
            ["Frontend", "Thymeleaf + Bootstrap CSS"],
            ["API Docs", "SpringDoc OpenAPI (Swagger UI)"],
            ["Messaging", "Apache Kafka 3.x (Confluent 7.6) + Transactional Outbox"],
            ["Functional style", "Vavr (Either, Option)"],
        ],
        [2.5*inch, 4*inch]
    ),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  2. TEAM CONTRIBUTIONS
# ═══════════════════════════════════════════════════════════
story += [
    P("2. Team Contributions", h1),
    hr(),
    P("""
    The project was developed collaboratively by a three-person team,
    with each member owning a distinct area of the system:
    """, body),
    sp(8),
    P("Maksim Ovchinnikov — Team Lead / Backend Engineer", h2),
    B("Designed and implemented the overall microservice architecture"),
    B("Developed IAC Service: JWT authentication, role management, token rotation, super admin seeding"),
    B("Developed Property Service: property CRUD, ownership validation, internal validate endpoint"),
    B("Developed Pricing Service: service type and addon catalog, PricingEngine with frequency discounts"),
    B("Developed Order Service: full order lifecycle, round-robin cleaner assignment, earnings tracking"),
    B("Developed Booking Service: integrated facade for properties, orders, catalog and earnings"),
    B("Configured Docker Compose: networking, health checks, environment variable injection"),
    B("Created Flyway database migrations for all services"),
    B("Set up Postman collection with automated test scripts and token management"),
    B("Authored README and project documentation"),
    sp(10),
    P("Harpreet Kaur — Frontend Developer", h2),
    B("Designed and implemented the UI Service (Thymeleaf + Spring MVC)"),
    B("Built the web gateway with reverse proxy routing to backend services"),
    B("Created login, registration, dashboard, properties, orders, and catalog pages"),
    B("Implemented client-side form validation and error handling"),
    B("Styled the application with Bootstrap and custom CSS"),
    B("Integrated frontend with IAC authentication flow (session/token management)"),
    B("Implemented role-based menu/page visibility in the UI"),
    sp(10),
    P("Pragati Sahani — QA / Tester", h2),
    B("Designed and executed the test plan covering all microservices"),
    B("Performed functional testing of all REST API endpoints using Postman"),
    B("Validated authentication flows: registration, login, token refresh, logout"),
    B("Tested role-based access control (RBAC) restrictions and edge cases"),
    B("Executed negative testing: invalid inputs, expired tokens, unauthorized access"),
    B("Validated order lifecycle: create → pay → complete → cancel"),
    B("Tested cleaner assignment round-robin behavior with multiple cleaners"),
    B("Verified pricing calculations for all service types, addons, and frequency discounts"),
    B("Documented test results and reported bugs to the development team"),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  3. SYSTEM ARCHITECTURE
# ═══════════════════════════════════════════════════════════
story += [
    P("3. System Architecture", h1),
    hr(),
    P("""
    The platform follows a <b>microservice architecture</b> where each service owns its domain,
    has its own Spring Boot application, and communicates with others via HTTP REST calls
    (for synchronous, request-response flows) and <b>Apache Kafka</b> (for asynchronous,
    event-driven flows). All services share a single PostgreSQL instance (with separate schemas)
    and communicate over a dedicated Docker bridge network (<code>cleaning-network</code>).
    """, body),
    sp(8),
    P("The <b>Transactional Outbox Pattern</b> ensures reliable event delivery: events are written to"
       " an <code>outbox_events</code> table in the same database transaction as the business operation."
       " A scheduled relay process polls the table and publishes events to Kafka, guaranteeing"
       " at-least-once delivery even if the service crashes between the write and the Kafka send.", body),
    sp(10),
    P("Service Port Map", h2),
    table(
        [
            ["Service", "Host Port", "Container Port", "Role"],
            ["PostgreSQL", "15432", "5432", "Shared relational database"],
            ["Zookeeper", "12181", "2181", "Kafka coordination"],
            ["Kafka Broker", "19092", "9092", "Async event bus"],
            ["IAC Service", "18080", "8080", "Identity & Access Control"],
            ["Property Service", "18081", "8080", "Client property management"],
            ["Pricing Service", "18082", "8080", "Catalog & price calculation"],
            ["Order Service", "18083", "8080", "Order lifecycle & cleaners"],
            ["UI / Booking Service", "18090", "8090", "Frontend + reverse proxy"],
        ],
        [1.8*inch, 1.1*inch, 1.2*inch, 2.7*inch]
    ),
    sp(14),
    P("Component Diagram", h2),
    sp(6),
    *diagram("component_diagram.png", "Full system component diagram showing inter-service communication"),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  4. SERVICE DESCRIPTIONS
# ═══════════════════════════════════════════════════════════
story += [P("4. Service Descriptions", h1), hr()]

# 4.1 IAC
story += [
    P("4.1 IAC Service — Identity & Access Control", h2),
    P("""
    The IAC Service is the security backbone of the platform. It manages user registration,
    authentication, JWT token issuance, refresh token rotation, and role-based access control.
    A super admin account is automatically seeded at startup via the <code>SuperAdminSeeder</code> component.
    """, body),
    P("Key Features:", h3),
    B("User registration with BCrypt password hashing"),
    B("JWT access tokens (15 min) + refresh tokens (7 days) with single-use rotation"),
    B("Roles: USER, CLIENT, CLEANER, ADMIN, SUPER_ADMIN"),
    B("All authenticated requests validated by JwtAuthFilter"),
    B("Admin endpoints: assign/revoke roles, list/delete users"),
    P("Entities:", h3),
    P("<b>UserEntity</b>: id, username, email, passwordHash, firstName, lastName, enabled, createdAt, updatedAt, roles (M:M), refreshTokens (1:M)", body),
    P("<b>RoleEntity</b>: id, name", body),
    P("<b>RefreshTokenEntity</b>: id, token, user, expiresAt", body),
    sp(10),
]

# 4.2 Property
story += [
    P("4.2 Property Service", h2),
    P("""
    Manages physical properties owned by clients. Each property represents a location to be cleaned
    (apartment, house, or office). Properties are identified by client ID (UUID derived from username)
    and support soft-deletion (active flag).
    """, body),
    P("Key Features:", h3),
    B("Property types: APARTMENT, HOUSE, OFFICE"),
    B("Area validation: minimum 10 sqm, bathrooms: 1–10"),
    B("Internal validation endpoint used by Order Service to confirm ownership"),
    B("Soft delete — deactivated properties excluded from lists"),
    sp(10),
]

# 4.3 Pricing
story += [
    P("4.3 Pricing Service", h2),
    P("""
    Provides a public service catalog (types and addons) and a price calculation engine.
    The PricingEngine computes the total price based on area, bathroom count, service type,
    selected addons, and frequency discount.
    """, body),
    P("Pricing Formula:", h3),
    P("<code>basePrice = areaSqm × ratePerSqm + bathroomsCount × ratePerBathroom</code>", code_style),
    P("<code>addonsPrice = sum(selectedAddon.price)</code>", code_style),
    P("<code>discount = frequencyDiscount (WEEKLY=10%, BI_WEEKLY=5%, MONTHLY=15%)</code>", code_style),
    P("<code>totalPrice = (basePrice + addonsPrice) × (1 - discount)</code>", code_style),
    P("Service Types:", h3),
    table(
        [
            ["Code", "Name", "Rate/sqm", "Rate/bathroom"],
            ["STANDARD", "Standard Cleaning", "$0.50", "$15.00"],
            ["DEEP", "Deep Cleaning", "$1.20", "$25.00"],
            ["MOVE_IN_OUT", "Move In/Out Cleaning", "$1.80", "$35.00"],
        ],
        [1.2*inch, 2*inch, 1.2*inch, 1.5*inch]
    ),
    sp(6),
    P("Sample Addons:", h3),
    table(
        [
            ["Code", "Name", "Price"],
            ["OVEN", "Oven Cleaning", "$40.00"],
            ["FRIDGE", "Fridge Cleaning", "$35.00"],
            ["WINDOWS", "Window Cleaning", "$50.00"],
            ["LAUNDRY", "Laundry & Folding", "$30.00"],
            ["IRONING", "Ironing", "$25.00"],
        ],
        [1.2*inch, 2.5*inch, 1.2*inch]
    ),
    sp(10),
]

# 4.4 Order
story += [
    P("4.4 Order Service", h2),
    P("""
    The Order Service handles the complete lifecycle of cleaning orders, from creation through payment
    to completion. It communicates with Property Service (ownership validation) and Pricing Service
    (price calculation) via HTTP. Cleaner assignment uses a <b>round-robin algorithm</b> based on
    the cleaner's last assignment timestamp.
    """, body),
    P("Order Status Flow:", h3),
    P("<code>PENDING → CONFIRMED → COMPLETED</code>  (or any status → CANCELLED)", code_style),
    P("Round-Robin Assignment:", h3),
    P("""
    When an order is paid, the system selects the available cleaner who was least recently assigned
    (ordered by <code>lastAssignedAt ASC</code>). If no cleaners are available, the field is left null
    and the admin must manually assign.
    """, body),
    P("Earnings:", h3),
    P("Upon order completion, an EarningEntity is created for the cleaner with 70% of the order's total price.", body),
    sp(10),
]

# 4.5 Booking
story += [
    P("4.5 Booking Service", h2),
    P("""
    The Booking Service is an integrated facade that combines property management, order management,
    and the service catalog into a single Spring Boot application. It uses its own PostgreSQL schema
    (<code>booking</code>) and is the primary backend used by the UI Service.
    """, body),
    P("Controllers:", h3),
    B("ServiceCatalogController — public: /api/v1/services, /api/v1/addons"),
    B("ClientController — ROLE_CLIENT: /api/v1/properties, /api/v1/orders"),
    B("CleanerController — ROLE_CLEANER: /api/v1/cleaner/*"),
    sp(10),
]

# 4.6 UI
story += [
    P("4.6 UI Service", h2),
    P("""
    The UI Service is a Spring Boot application with Thymeleaf templating that serves the web
    frontend and acts as a reverse proxy to the backend services. It maintains user session state
    after authentication and provides role-based navigation.
    """, body),
    B("ProxyController: routes API calls from the browser to the appropriate backend service"),
    B("HomeController: serves Thymeleaf pages (dashboard, login, properties, orders, catalog)"),
    B("Session management: stores JWT token in HTTP session after login"),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  5. DOMAIN MODEL
# ═══════════════════════════════════════════════════════════
story += [
    P("5. Domain Model", h1),
    hr(),
    P("""
    The following class diagram shows the core domain entities across all services and their
    relationships. Note that while services share a PostgreSQL instance, each service manages
    its own schema and entities independently.
    """, body),
    sp(10),
    *diagram("class_diagram.png", "Domain Entity Model — all services"),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  6. MESSAGING ARCHITECTURE (KAFKA + OUTBOX PATTERN)
# ═══════════════════════════════════════════════════════════
story += [
    P("6. Messaging Architecture — Kafka + Transactional Outbox", h1),
    hr(),
    P("""
    To achieve <b>reliable, decoupled inter-service communication</b>, the platform uses
    <b>Apache Kafka</b> as the event bus combined with the <b>Transactional Outbox Pattern</b>.
    This guarantees that events are never lost — even if a service crashes between processing
    a request and publishing to Kafka.
    """, body),
    sp(8),
    P("Outbox Pattern — How It Works", h2),
    B("<b>Step 1 (Atomic):</b> Business logic writes both the domain record AND an outbox_events row in a single DB transaction"),
    B("<b>Step 2 (Async):</b> OutboxRelayScheduler polls outbox_events WHERE status=PENDING every 5 seconds"),
    B("<b>Step 3 (Publish):</b> Relay publishes each event to Kafka and marks it PROCESSED"),
    B("<b>Retry:</b> On Kafka failure, event stays PENDING and is retried on next poll cycle"),
    sp(10),
    P("Kafka Topics", h2),
    table(
        [
            ["Topic", "Producer", "Consumer(s)", "Trigger"],
            ["user.registered", "IAC Service", "Order Service", "New user registration"],
            ["user.role_assigned", "IAC Service", "Order Service", "Admin assigns CLEANER role"],
            ["user.role_revoked", "IAC Service", "(future)", "Admin revokes a role"],
            ["order.created", "Order Service", "(future notifications)", "Order placed by client"],
            ["order.confirmed", "Order Service", "(future notifications)", "Order paid + cleaner assigned"],
            ["order.completed", "Order Service", "(future analytics)", "Cleaner marks order complete"],
            ["order.cancelled", "Order Service", "(future notifications)", "Client cancels order"],
        ],
        [1.4*inch, 1.2*inch, 1.4*inch, 2.7*inch]
    ),
    sp(10),
    P("Consumer: Order Service — UserEventConsumer", h2),
    P("""
    The Order Service consumes <code>user.registered</code> and <code>user.role_assigned</code>
    events to automatically create a <code>CleanerProfile</code> for any user who registers as
    or is assigned the CLEANER role. This eliminates the previous dependency on the cleaner
    making their first API call before being available for round-robin assignment.
    """, body),
    sp(10),
    P("Outbox Pattern Sequence", h2),
    sp(6),
    *diagram("sequence_kafka_outbox.png", "Transactional Outbox Pattern — from business transaction to Kafka delivery"),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  7. AUTHENTICATION & SECURITY
# ═══════════════════════════════════════════════════════════
story += [
    P("7. Authentication & Security", h1),
    hr(),
    P("""
    All services (except public catalog endpoints) are protected by JWT Bearer token authentication.
    The same JWT secret is shared across all services via the <code>JWT_SECRET</code> environment variable,
    allowing any service to independently validate tokens issued by the IAC Service.
    """, body),
    sp(8),
    P("Authentication Flow", h2),
    sp(6),
    *diagram("sequence_login.png", "Login and JWT token flow — full sequence"),
    sp(10),
    P("Security Configuration", h2),
    P("Public endpoints (no auth required):", h3),
    B("POST /api/v1/auth/register"),
    B("POST /api/v1/auth/login"),
    B("POST /api/v1/auth/refresh"),
    B("GET /api/v1/catalog/services"),
    B("GET /api/v1/catalog/addons"),
    B("GET /api/v1/services"),
    B("GET /api/v1/addons"),
    B("GET /swagger-ui/** and /v3/api-docs/**"),
    sp(6),
    P("""
    All other endpoints require a valid JWT Bearer token in the <code>Authorization</code> header:
    <code>Authorization: Bearer &lt;accessToken&gt;</code>. Method-level security
    is enforced with <code>@PreAuthorize</code> annotations.
    """, body),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  7. API REFERENCE
# ═══════════════════════════════════════════════════════════
story += [
    P("7. API Reference", h1),
    hr(),
    P("IAC Service (:18080)", h2),
    table(
        [
            ["Method", "Path", "Auth", "Description"],
            ["POST", "/api/v1/auth/register", "None", "Register new user"],
            ["POST", "/api/v1/auth/login", "None", "Login, returns JWT tokens"],
            ["POST", "/api/v1/auth/refresh", "None", "Refresh access token"],
            ["POST", "/api/v1/auth/logout", "Bearer", "Revoke all refresh tokens"],
            ["GET", "/api/v1/users/me", "Bearer", "Get own profile"],
            ["GET", "/api/v1/users", "ADMIN+", "List users (paginated)"],
            ["GET", "/api/v1/users/{id}", "ADMIN+", "Get user by UUID"],
            ["DELETE", "/api/v1/users/{id}", "SUPER_ADMIN", "Delete user"],
            ["GET", "/api/v1/admin/roles", "ADMIN+", "List available roles"],
            ["POST", "/api/v1/admin/assign-role", "ADMIN+", "Assign role to user"],
            ["POST", "/api/v1/admin/revoke-role", "ADMIN+", "Revoke role from user"],
        ],
        [0.7*inch, 2.3*inch, 1.1*inch, 2.7*inch]
    ),
    sp(10),
    P("Property Service (:18081)", h2),
    table(
        [
            ["Method", "Path", "Auth", "Description"],
            ["POST", "/api/v1/properties", "CLIENT", "Add property"],
            ["GET", "/api/v1/properties", "Bearer", "List my properties"],
            ["GET", "/api/v1/properties/{id}", "Bearer", "Get property by ID"],
            ["PUT", "/api/v1/properties/{id}", "CLIENT", "Update property"],
            ["DELETE", "/api/v1/properties/{id}", "CLIENT", "Soft-delete property"],
            ["GET", "/api/v1/properties/{id}/validate", "Bearer", "[Internal] Validate ownership"],
        ],
        [0.7*inch, 2.3*inch, 1.1*inch, 2.7*inch]
    ),
    sp(10),
    P("Pricing Service (:18082)", h2),
    table(
        [
            ["Method", "Path", "Auth", "Description"],
            ["GET", "/api/v1/catalog/services", "None", "List service types with rates"],
            ["GET", "/api/v1/catalog/addons", "None", "List available addons"],
            ["POST", "/api/v1/pricing/calculate", "None", "Calculate order price"],
        ],
        [0.7*inch, 2.3*inch, 1.1*inch, 2.7*inch]
    ),
    sp(10),
    P("Order Service (:18083) — Client", h2),
    table(
        [
            ["Method", "Path", "Auth", "Description"],
            ["POST", "/api/v1/orders", "CLIENT", "Create order (PENDING)"],
            ["POST", "/api/v1/orders/{id}/pay", "CLIENT", "Pay order → CONFIRMED"],
            ["GET", "/api/v1/orders", "Bearer", "List my orders"],
            ["GET", "/api/v1/orders/{id}", "Bearer", "Get order details"],
            ["DELETE", "/api/v1/orders/{id}", "CLIENT", "Cancel order"],
        ],
        [0.7*inch, 2.3*inch, 1.1*inch, 2.7*inch]
    ),
    sp(10),
    P("Order Service (:18083) — Cleaner", h2),
    table(
        [
            ["Method", "Path", "Auth", "Description"],
            ["GET", "/api/v1/cleaner/schedule", "CLEANER", "My upcoming jobs"],
            ["PUT", "/api/v1/cleaner/profile/availability", "CLEANER", "Toggle availability"],
            ["POST", "/api/v1/cleaner/orders/{id}/complete", "CLEANER", "Mark job complete"],
            ["POST", "/api/v1/cleaner/orders/{id}/request-replacement", "CLEANER", "Request replacement"],
            ["GET", "/api/v1/cleaner/earnings", "CLEANER", "Total earnings"],
            ["GET", "/api/v1/cleaner/earnings/{year}/{month}", "CLEANER", "Monthly earnings"],
        ],
        [0.7*inch, 2.5*inch, 1.1*inch, 2.5*inch]
    ),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  8. SEQUENCE DIAGRAMS
# ═══════════════════════════════════════════════════════════
story += [
    P("8. Sequence Diagrams", h1),
    hr(),
    P("8.1 Order Lifecycle — Create, Pay, Complete", h2),
    sp(6),
    *diagram("sequence_order_flow.png", "Full order lifecycle from creation through payment to completion"),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  9. POSTMAN COLLECTION
# ═══════════════════════════════════════════════════════════
story += [
    P("9. Postman Collection", h1),
    hr(),
    P("""
    A complete Postman collection is included in the <code>postman/</code> directory.
    It covers all endpoints across all 5 services with example request bodies,
    response examples, and automated test scripts.
    """, body),
    sp(8),
    P("Collection Files", h2),
    table(
        [
            ["File", "Description"],
            ["CleaningPlatform_Full.postman_collection.json", "Full collection — all 5 services, 40+ requests"],
            ["IAC_Service.postman_collection.json", "IAC Service only (legacy)"],
        ],
        [3.2*inch, 3.6*inch]
    ),
    sp(10),
    P("Collection Folders", h2),
    table(
        [
            ["Folder", "Endpoints", "Notes"],
            ["IAC Service — Authentication", "6", "Register, login (admin/user/cleaner), refresh, logout"],
            ["IAC Service — Users", "4", "Get profile, list users, get by ID, delete"],
            ["IAC Service — Admin", "5", "List roles, assign, revoke (including error case)"],
            ["Property Service", "7", "CRUD + internal validate endpoint"],
            ["Pricing Service", "5", "Catalog (services/addons), price calc (3 variants)"],
            ["Order Service — Client", "5", "Create, pay, list, get, cancel"],
            ["Order Service — Cleaner", "7", "Schedule, availability, complete, replacement, earnings"],
            ["Booking Service", "13", "Full client + cleaner + catalog via UI proxy"],
        ],
        [2.4*inch, 0.8*inch, 3.6*inch]
    ),
    sp(10),
    P("Token Auto-Save Scripts", h2),
    P("""
    Every login, register, and refresh request includes a Post-Response Script that automatically
    saves tokens to collection variables so all subsequent requests are pre-authorized:
    """, body),
    P("""var json = pm.response.json();\nif (json.data && json.data.accessToken) {\n  pm.collectionVariables.set('accessToken',  json.data.accessToken);\n  pm.collectionVariables.set('refreshToken', json.data.refreshToken);\n  console.log('Tokens saved. Length:', json.data.accessToken.length);\n}""", code_style),
    sp(10),
    P("Recommended Test Workflow", h2),
    P("1. Import collection into Postman", body),
    P("2. Run <b>Login as Super Admin</b> — tokens auto-saved", body),
    P("3. Run <b>Register New User</b> — creates 'john_doe'", body),
    P("4. Run <b>Assign CLEANER role</b> to 'cleaner_bob'", body),
    P("5. Run <b>Add Property</b> (as CLIENT) — propertyId auto-saved", body),
    P("6. Run <b>Calculate Price</b> — verify pricing engine", body),
    P("7. Run <b>Create Order</b> — orderId auto-saved; status=PENDING", body),
    P("8. Run <b>Pay Order</b> — status=CONFIRMED; cleaner assigned", body),
    P("9. Switch to Cleaner login, run <b>My Schedule</b>", body),
    P("10. Run <b>Complete Order</b> — status=COMPLETED; earning recorded", body),
    P("11. Run <b>Total Earnings</b> — verify amount (70% of total price)", body),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  10. DEPLOYMENT
# ═══════════════════════════════════════════════════════════
story += [
    P("10. Deployment", h1),
    hr(),
    P("""
    The entire platform is containerized and can be started with a single Docker Compose command.
    Each microservice has its own <code>Dockerfile</code> using a multi-stage build to minimize image size.
    """, body),
    sp(8),
    P("Starting the Platform", h2),
    P("docker compose up --build", code_style),
    sp(6),
    P("Services start in dependency order:", body),
    B("1. PostgreSQL — waits for pg_isready health check"),
    B("2. IAC Service — migrates schema, seeds super admin"),
    B("3. Property, Pricing, Order Services — start concurrently after PostgreSQL"),
    B("4. UI/Booking Service — starts last, connects to all backend services"),
    sp(10),
    P("Environment Configuration", h2),
    P("""
    All sensitive configuration is externalized to environment variables defined in
    <code>docker-compose.yml</code>. For production, these should be moved to a secrets manager
    or <code>.env</code> file (excluded from Git via <code>.gitignore</code>).
    """, body),
    table(
        [
            ["Variable", "Default", "Description"],
            ["JWT_SECRET", "(long string)", "Shared JWT signing secret — CHANGE IN PROD"],
            ["APP_SUPERADMIN_USERNAME", "admin", "Bootstrap super admin username"],
            ["APP_SUPERADMIN_PASSWORD", "password", "Bootstrap super admin password"],
            ["JWT_ACCESS_TOKEN_EXPIRY_MS", "900000", "Access token TTL (15 min)"],
            ["JWT_REFRESH_TOKEN_EXPIRY_MS", "604800000", "Refresh token TTL (7 days)"],
            ["SERVICES_PRICING_URL", "http://pricing-service:8080", "Internal pricing URL"],
            ["SERVICES_PROPERTY_URL", "http://property-service:8080", "Internal property URL"],
        ],
        [2.3*inch, 1.8*inch, 2.7*inch]
    ),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  11. TESTING
# ═══════════════════════════════════════════════════════════
story += [
    P("11. Testing", h1),
    hr(),
    P("""
    Testing was performed by Pragati Sahani using the Postman collection against a locally running
    Docker Compose environment. The test plan covered positive flows, negative/validation cases,
    security boundary tests, and inter-service integration scenarios.
    """, body),
    sp(8),
    P("Test Coverage Summary", h2),
    table(
        [
            ["Area", "Tests", "Result"],
            ["User Registration", "Valid input, duplicate username, weak password", "PASS"],
            ["Authentication", "Login (admin/user/cleaner), invalid credentials", "PASS"],
            ["Token Management", "Refresh rotation, expired token handling, logout", "PASS"],
            ["Role Management", "Assign/revoke roles, SUPER_ADMIN protection", "PASS"],
            ["Property CRUD", "Add, list, update, delete, not-owned access denied", "PASS"],
            ["Pricing Engine", "STANDARD/DEEP/MOVE_IN_OUT, addons, frequency discounts", "PASS"],
            ["Order Creation", "Valid order, invalid property, pricing integration", "PASS"],
            ["Order Payment", "Round-robin assignment, no-cleaner fallback", "PASS"],
            ["Order Completion", "Status transition, earning record creation", "PASS"],
            ["Cleaner Endpoints", "Schedule, availability toggle, earnings by month", "PASS"],
            ["RBAC Enforcement", "CLIENT-only, CLEANER-only, ADMIN-only access denied", "PASS"],
            ["Input Validation", "Missing fields, bad UUID, past scheduled date", "PASS"],
        ],
        [2.2*inch, 2.8*inch, 1.4*inch]
    ),
    sp(10),
    P("Running Tests with Newman (CLI)", h2),
    P("""npm install -g newman\nnewman run postman/CleaningPlatform_Full.postman_collection.json --reporters cli,json""", code_style),
    PageBreak()
]

# ═══════════════════════════════════════════════════════════
#  12. CONCLUSION
# ═══════════════════════════════════════════════════════════
story += [
    P("12. Conclusion", h1),
    hr(),
    P("""
    The Cleaning Platform successfully demonstrates a production-grade microservice application
    using modern Java enterprise technologies. The project achieves the following learning outcomes:
    """, body),
    sp(8),
    B("<b>Microservice Architecture:</b> 5 independently deployable services with clear bounded contexts"),
    B("<b>Security:</b> Stateless JWT authentication, role-based access control, refresh token rotation"),
    B("<b>Inter-Service Communication:</b> Synchronous REST HTTP calls with error propagation"),
    B("<b>Database Design:</b> Normalized relational schema, JPA mappings, Flyway migrations"),
    B("<b>Docker Orchestration:</b> Multi-service Compose with health checks and dependency ordering"),
    B("<b>API Design:</b> RESTful endpoints, consistent response envelopes, OpenAPI documentation"),
    B("<b>Business Logic:</b> Price calculation engine, round-robin scheduling, earnings tracking"),
    B("<b>Team Collaboration:</b> Clear role separation across backend, frontend, and testing"),
    sp(16),
    P("""
    The platform is fully functional, passes all test cases, and is ready for demonstration
    and assessment. Future enhancements could include asynchronous notification via message queues,
    calendar integration for scheduling, payment gateway integration, and a mobile client.
    """, body),
    sp(30),
    hr(),
    P("Humber College — J2EE Enterprise Application Development — April 2026", ParagraphStyle('footer', fontSize=10, fontName='Helvetica', alignment=TA_CENTER, textColor=colors.HexColor('#888888'))),
]

# ─────────────────────────────────────────────────────────
#  Build
# ─────────────────────────────────────────────────────────
doc.build(story)
print(f"✅ PDF generated: {OUTPUT_PDF}")
print(f"   Size: {os.path.getsize(OUTPUT_PDF):,} bytes")
