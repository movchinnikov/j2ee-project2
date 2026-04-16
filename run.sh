#!/usr/bin/env bash
# ============================================================
# run.sh — J2EE Project Launcher
# Usage:
#   sh ./run.sh           — build and start all services
#   sh ./run.sh stop      — stop all services
#   sh ./run.sh logs      — tail logs
#   sh ./run.sh status    — show container status
#   sh ./run.sh clean     — stop and remove volumes
# ============================================================

COMPOSE_FILE="docker-compose.yml"
PROJECT_NAME="j2ee-project2"

# ── Ensure Docker CLI is reachable (Docker Desktop on macOS) ──
export PATH="/usr/local/bin:/Applications/Docker.app/Contents/Resources/bin:$PATH"

log_info()    { printf "[INFO]  %s\n" "$*"; }
log_success() { printf "[OK]    %s\n" "$*"; }
log_warn()    { printf "[WARN]  %s\n" "$*"; }

# Verify docker is available
if ! command -v docker &>/dev/null; then
    printf "[ERROR] docker command not found. Is Docker Desktop running?\n"
    exit 1
fi

start() {
    printf "\n"
    printf "============================================================\n"
    printf "  J2EE Project — Cleaning Platform\n"
    printf "============================================================\n\n"

    # Kill any leftover booking-service that holds port 18081
    docker stop booking-service 2>/dev/null && docker rm booking-service 2>/dev/null && log_info "Removed orphan booking-service"

    log_info "Building images (this takes ~2 min on first run)..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" build

    log_info "Starting all services in background..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d --remove-orphans

    printf "\n"
    printf "============================================================\n"
    printf "  Services started!\n"
    printf "  IAC Service      : http://localhost:18080\n"
    printf "  Swagger (IAC)    : http://localhost:18080/swagger-ui.html\n"
    printf "  Property Service : http://localhost:18081/swagger-ui.html\n"
    printf "  Pricing Service  : http://localhost:18082/swagger-ui.html\n"
    printf "  Order Service    : http://localhost:18083/swagger-ui.html\n"
    printf "  UI Service       : http://localhost:18090\n"
    printf "  PostgreSQL       : localhost:15432  (iac_user / iac_db)\n"
    printf "  Admin login      : admin / password\n"
    printf "============================================================\n"
    printf "\n"
    printf "  TIP: watch logs with:  sh ./run.sh logs\n"
    printf "  TIP: check status:     sh ./run.sh status\n\n"
}

stop() {
    log_info "Stopping all services..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down --remove-orphans
    log_success "Services stopped."
}

tail_logs() {
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f --tail=50
}

status() {
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps
}

clean() {
    log_warn "This will remove all containers AND volumes!"
    printf "Are you sure? (y/N): "
    read -r confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v --remove-orphans
        log_success "Cleaned."
    else
        log_info "Cancelled."
    fi
}

case "${1:-start}" in
    start)   start ;;
    stop)    stop ;;
    logs)    tail_logs ;;
    status)  status ;;
    clean)   clean ;;
    *)
        printf "Usage: %s {start|stop|logs|status|clean}\n" "$0"
        exit 1
        ;;
esac
