#!/usr/bin/env bash
# scripts/check_docs.sh
# Verifies documentation parity between AirMouse codebase (Go server & protocol implementations)
# and the canonical protocol specification in docs/PROTOCOL_V2.md and DOCUMENTATION.md.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}        AirMouse Documentation Parity Checker         ${NC}"
echo -e "${BLUE}======================================================${NC}"

DOCS_V2="${ROOT_DIR}/docs/PROTOCOL_V2.md"
MASTER_DOC="${ROOT_DIR}/DOCUMENTATION.md"
GO_MESSAGE_FILE="${ROOT_DIR}/pc/airmouse_go_new/internal/protocol/message.go"
TCP_SERVER_FILE="${ROOT_DIR}/pc/airmouse_go_new/internal/protocol/tcp/server.go"
WS_SERVER_FILE="${ROOT_DIR}/pc/airmouse_go_new/internal/protocol/websocket/server.go"

if [ ! -f "${DOCS_V2}" ]; then
    echo -e "${RED}[ERROR] docs/PROTOCOL_V2.md not found!${NC}"
    exit 1
fi

if [ ! -f "${MASTER_DOC}" ]; then
    echo -e "${RED}[ERROR] DOCUMENTATION.md not found!${NC}"
    exit 1
fi

if [ ! -f "${GO_MESSAGE_FILE}" ]; then
    echo -e "${RED}[ERROR] Go protocol message file (${GO_MESSAGE_FILE}) not found!${NC}"
    exit 1
fi

echo -e "\n${YELLOW}[1/4] Checking Protocol Envelope Definitions...${NC}"
REQUIRED_ENVELOPE_FIELDS=("\"v\"" "\"type\"" "\"seq\"" "\"ts\"" "\"payload\"")
for field in "${REQUIRED_ENVELOPE_FIELDS[@]}"; do
    if grep -q "${field}" "${DOCS_V2}"; then
        echo -e "  ${GREEN}✓${NC} Envelope field ${field} documented in PROTOCOL_V2.md"
    else
        echo -e "  ${RED}✗${NC} Envelope field ${field} MISSING in PROTOCOL_V2.md"
        exit 1
    fi
    if grep -q "${field}" "${MASTER_DOC}"; then
        echo -e "  ${GREEN}✓${NC} Envelope field ${field} documented in DOCUMENTATION.md"
    else
        echo -e "  ${RED}✗${NC} Envelope field ${field} MISSING in DOCUMENTATION.md"
        exit 1
    fi
done

echo -e "\n${YELLOW}[2/4] Extracting Message Types from Go Codebase...${NC}"

# Extract types from IsValidType in message.go
GO_TYPES=$(awk '/func IsValidType/,/^}/' "${GO_MESSAGE_FILE}" | grep -E '"[a-z_]+":' | sed -E 's/.*"([a-z_]+)":.*/\1/' | sort -u)

# Additional protocol types handled across servers
CORE_HANDLED_TYPES=("move" "click" "mouse_down" "mouse_up" "doubleclick" "rightclick" "scroll" "hello" "ping" "pong" "ack" "nack" "gesture" "proximity" "control" "presentation" "media" "laser" "key" "keypress" "type" "smarthome" "steering" "shooter" "calibration_data" "voice_command" "discover" "offer" "welcome" "status" "file" "battery" "log" "custom" "stats" "notification" "config" "error")

ALL_TYPES=$(echo -e "${GO_TYPES}\n$(printf '%s\n' "${CORE_HANDLED_TYPES[@]}")" | sort -u | grep -v '^$')

TOTAL_TYPES=$(echo "${ALL_TYPES}" | wc -l | tr -d ' ')
echo -e "  Found ${TOTAL_TYPES} active protocol message types in codebase."

echo -e "\n${YELLOW}[3/4] Validating Message Type Parity with docs/PROTOCOL_V2.md...${NC}"
MISSING_V2=0
for msg_type in ${ALL_TYPES}; do
    if grep -qE "(\"type\":[[:space:]]*\"${msg_type}\"|####[[:space:]]*\`${msg_type}\`|\`${msg_type}\`)" "${DOCS_V2}"; then
        echo -e "  ${GREEN}✓${NC} Message type '${msg_type}' is documented in PROTOCOL_V2.md"
    else
        echo -e "  ${RED}✗${NC} Message type '${msg_type}' MISSING from docs/PROTOCOL_V2.md!"
        MISSING_V2=$((MISSING_V2 + 1))
    fi
done

echo -e "\n${YELLOW}[4/4] Validating Message Type Parity with DOCUMENTATION.md...${NC}"
MISSING_MASTER=0
for msg_type in ${ALL_TYPES}; do
    if grep -qE "(\"type\":[[:space:]]*\"${msg_type}\"|####[[:space:]]*\`${msg_type}\`|\`${msg_type}\`)" "${MASTER_DOC}"; then
        echo -e "  ${GREEN}✓${NC} Message type '${msg_type}' is documented in DOCUMENTATION.md"
    else
        echo -e "  ${RED}✗${NC} Message type '${msg_type}' MISSING from DOCUMENTATION.md!"
        MISSING_MASTER=$((MISSING_MASTER + 1))
    fi
done

echo -e "\n${BLUE}======================================================${NC}"
if [ ${MISSING_V2} -eq 0 ] && [ ${MISSING_MASTER} -eq 0 ]; then
    echo -e "${GREEN}✓ ALL ${TOTAL_TYPES} PROTOCOL MESSAGE TYPES ARE SYNCHRONIZED AND VERIFIED!${NC}"
    echo -e "${BLUE}======================================================${NC}"
    exit 0
else
    echo -e "${RED}✗ PARITY CHECK FAILED: ${MISSING_V2} missing in PROTOCOL_V2.md, ${MISSING_MASTER} missing in DOCUMENTATION.md${NC}"
    echo -e "${BLUE}======================================================${NC}"
    exit 1
fi
