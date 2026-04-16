#!/usr/bin/env bash
# ============================================================
# destroy.sh — NUCLEAR RESET
# Completely tears down all Docker resources for this project:
#   • Stops and removes all containers (both project names)
#   • Removes named volumes (postgres data)
#   • Removes project images
#   • Removes the shared network
#
# Usage:  sh ./destroy.sh
# ============================================================

set -e

# ── Ensure Docker CLI is reachable (Docker Desktop on macOS) ──
export PATH="/usr/local/bin:/Applications/Docker.app/Contents/Resources/bin:$PATH"

RED='\033[0;31m'
YEL='\033[1;33m'
GRN='\033[0;32m'
NC='\033[0m'

warn()    { printf "${YEL}[WARN]${NC}  %s\n" "$*"; }
ok()      { printf "${GRN}[OK]${NC}    %s\n" "$*"; }
section() { printf "\n${RED}══ %s ══${NC}\n" "$*"; }

if ! command -v docker &>/dev/null; then
    printf "[ERROR] docker command not found. Is Docker Desktop running?\n"
    exit 1
fi

COMPOSE_FILE="docker-compose.yml"

printf "\n"
printf "${RED}╔══════════════════════════════════════════════════════╗${NC}\n"
printf "${RED}║         NUCLEAR DESTROY — ALL DATA WILL BE LOST      ║${NC}\n"
printf "${RED}╚══════════════════════════════════════════════════════╝${NC}\n\n"

warn "This will PERMANENTLY delete:"
warn "  • All containers (postgres, iac, property, pricing, order, ui, kafka, zookeeper)"
warn "  • All volumes    (postgres data — ALL databases)"
warn "  • All images     (project Docker images)"
warn "  • The Docker network (cleaning-network)"
printf "\n"
printf "Are you absolutely sure? Type 'yes' to confirm: "
read -r confirm

if [ "$confirm" != "yes" ]; then
    printf "[INFO]  Cancelled — nothing was changed.\n"
    exit 0
fi

# ── Step 1: Stop & remove via docker compose (both project names) ──
section "Stopping Compose stacks"

for proj in "j2ee-project2" "j2ee-project"; do
    if docker compose -f "$COMPOSE_FILE" -p "$proj" ps -q 2>/dev/null | grep -q .; then
        warn "Stopping project: $proj"
        docker compose -f "$COMPOSE_FILE" -p "$proj" down --remove-orphans 2>/dev/null || true
        ok "Stopped: $proj"
    else
        printf "[INFO]  No running containers for project: %s\n" "$proj"
    fi
done

# ── Step 2: Force-remove individual containers by name ────────────
section "Removing containers by name"

CONTAINERS=(
    "cleaning-postgres"
    "iac-service"
    "property-service"
    "pricing-service"
    "order-service"
    "iac-ui"
    "cleaning-zookeeper"
    "cleaning-kafka"
    "booking-service"
)

for c in "${CONTAINERS[@]}"; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${c}$"; then
        docker rm -f "$c" 2>/dev/null && ok "Removed container: $c" || warn "Could not remove: $c"
    else
        printf "[INFO]  Container not found (already gone): %s\n" "$c"
    fi
done

# ── Step 3: Remove volumes ────────────────────────────────────────
section "Removing volumes"

VOLUMES=(
    "cleaning-postgres-data"
)

for v in "${VOLUMES[@]}"; do
    if docker volume ls -q | grep -q "^${v}$"; then
        docker volume rm "$v" 2>/dev/null && ok "Removed volume: $v" || warn "Could not remove volume: $v"
    else
        printf "[INFO]  Volume not found (already gone): %s\n" "$v"
    fi
done

# Also remove any compose-namespaced volumes
for proj in "j2ee-project2" "j2ee-project"; do
    docker compose -f "$COMPOSE_FILE" -p "$proj" down -v --remove-orphans 2>/dev/null || true
done

# ── Step 4: Remove project Docker images ─────────────────────────
section "Removing project images"

IMAGE_PREFIXES=(
    "j2ee-project2-"
    "j2ee-project-"
    "j2ee-project_"
)

for prefix in "${IMAGE_PREFIXES[@]}"; do
    images=$(docker images --format '{{.Repository}}:{{.Tag}}' | grep "^${prefix}" 2>/dev/null || true)
    if [ -n "$images" ]; then
        echo "$images" | xargs docker rmi -f 2>/dev/null && ok "Removed images with prefix: $prefix" || warn "Some images could not be removed"
    else
        printf "[INFO]  No images found with prefix: %s\n" "$prefix"
    fi
done

# ── Step 5: Remove the shared network ────────────────────────────
section "Removing network"

if docker network ls --format '{{.Name}}' | grep -q "^cleaning-network$"; then
    docker network rm cleaning-network 2>/dev/null && ok "Removed network: cleaning-network" || warn "Network removal failed (containers still attached?)"
else
    printf "[INFO]  Network not found (already gone): cleaning-network\n"
fi

# ── Done ──────────────────────────────────────────────────────────
printf "\n"
printf "${GRN}╔══════════════════════════════════════════════════════╗${NC}\n"
printf "${GRN}║         DESTROY COMPLETE — Clean slate achieved!     ║${NC}\n"
printf "${GRN}╚══════════════════════════════════════════════════════╝${NC}\n\n"
printf "  To start fresh:  sh ./run.sh\n\n"
