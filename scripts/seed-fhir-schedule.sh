#!/bin/bash

set -e

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin@hdim.ai}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440010}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,EVALUATOR}"
USE_TRUSTED_HEADERS="${USE_TRUSTED_HEADERS:-true}"
API_TOKEN="${API_TOKEN:-}"

IDENTIFIER_SYSTEM="urn:hdim-demo"
TODAY_UTC="$(date -u +%Y-%m-%d)"
DAYS_AHEAD="${DAYS_AHEAD:-2}"
SEED_SCHEDULE_MODE="${SEED_SCHEDULE_MODE:-both}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

AUTH_HEADER=()
if [ "$USE_TRUSTED_HEADERS" = "true" ]; then
  VALIDATED_TS=$(date +%s)
  AUTH_HEADER=(
    -H "X-Auth-User-Id: $AUTH_USER_ID"
    -H "X-Auth-Username: $AUTH_USERNAME"
    -H "X-Auth-Roles: $AUTH_ROLES"
    -H "X-Auth-Tenant-Ids: $TENANT_ID"
    -H "X-Auth-Validated: gateway-${VALIDATED_TS}-dev"
  )
else
  if [ -z "$API_TOKEN" ]; then
    LOGIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}")
    API_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken // empty')
  fi
  if [ -n "$API_TOKEN" ]; then
    AUTH_HEADER=(-H "Authorization: Bearer $API_TOKEN")
  fi
fi

urlencode() {
  python3 - << 'PYEOF' "$1"
import sys, urllib.parse
print(urllib.parse.quote(sys.argv[1]))
PYEOF
}

fhir_get() {
  local path="$1"
  curl -s -H "X-Tenant-ID: $TENANT_ID" "${AUTH_HEADER[@]}" "$FHIR_URL/$path"
}

fhir_post() {
  local resource="$1"
  local payload="$2"
  curl -s -X POST "$FHIR_URL/$resource" \
    -H "Content-Type: application/fhir+json" \
    -H "X-Tenant-ID: $TENANT_ID" \
    "${AUTH_HEADER[@]}" \
    -d "$payload"
}

uuid_for() {
  local key="$1"
  python3 - << 'PYEOF' "$key"
import sys, uuid
print(uuid.uuid5(uuid.NAMESPACE_DNS, sys.argv[1]))
PYEOF
}

fhir_exists() {
  local resource="$1"
  local id="$2"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" -H "X-Tenant-ID: $TENANT_ID" "${AUTH_HEADER[@]}" "$FHIR_URL/$resource/$id")
  if [ "$status" = "200" ]; then
    return 0
  fi
  return 1
}

should_seed() {
  local mode="$1"
  case "$SEED_SCHEDULE_MODE" in
    both) return 0 ;;
    appointment-task) [ "$mode" = "appointment-task" ] && return 0 ;;
    encounter) [ "$mode" = "encounter" ] && return 0 ;;
  esac
  return 1
}

get_id_by_identifier() {
  local resource="$1"
  local identifier="$2"
  local encoded
  encoded=$(urlencode "${identifier}")
  fhir_get "${resource}?identifier=${encoded}" | jq -r '.entry[0].resource.id // empty' 2>/dev/null || true
}

ensure_resource() {
  local resource="$1"
  local identifier="$2"
  local payload="$3"
  local id
  id=$(get_id_by_identifier "$resource" "$identifier")
  if [ -n "$id" ]; then
    echo "$id"
    return
  fi
  id=$(fhir_post "$resource" "$payload" | jq -r '.id // empty' 2>/dev/null || true)
  echo "$id"
}

iso_time() {
  local day="$1"
  local time="$2"
  python3 - << 'PYEOF' "$day" "$time"
import sys
from datetime import datetime, timezone

day, time = sys.argv[1], sys.argv[2]
dt = datetime.strptime(f"{day} {time}", "%Y-%m-%d %H:%M").replace(tzinfo=timezone.utc)
print(dt.strftime("%Y-%m-%dT%H:%M:00Z"))
PYEOF
}

iso_time_plus_minutes() {
  local day="$1"
  local time="$2"
  local minutes="$3"
  python3 - << 'PYEOF' "$day" "$time" "$minutes"
import sys
from datetime import datetime, timedelta, timezone

day, time, minutes = sys.argv[1], sys.argv[2], int(sys.argv[3])
dt = datetime.strptime(f"{day} {time}", "%Y-%m-%d %H:%M").replace(tzinfo=timezone.utc)
dt = dt + timedelta(minutes=minutes)
print(dt.strftime("%Y-%m-%dT%H:%M:00Z"))
PYEOF
}

day_for_offset() {
  local offset="$1"
  python3 - << 'PYEOF' "$TODAY_UTC" "$offset"
import sys
from datetime import datetime, timedelta

base, offset = sys.argv[1], int(sys.argv[2])
dt = datetime.strptime(base, "%Y-%m-%d") + timedelta(days=offset)
print(dt.strftime("%Y-%m-%d"))
PYEOF
}

echo "Seeding FHIR scheduling data for ${TODAY_UTC} (next ${DAYS_AHEAD} days)..."

org_id=$(ensure_resource "Organization" "${IDENTIFIER_SYSTEM}|org-main-street" "{
  \"resourceType\": \"Organization\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"org-main-street\"}],
  \"name\": \"Main Street Clinic\"
}")

location_id=$(ensure_resource "Location" "${IDENTIFIER_SYSTEM}|loc-main-street" "{
  \"resourceType\": \"Location\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"loc-main-street\"}],
  \"name\": \"Main Street Clinic - Suite 200\",
  \"managingOrganization\": {\"reference\": \"Organization/${org_id}\"}
}")

provider_id=$(ensure_resource "Practitioner" "${IDENTIFIER_SYSTEM}|prac-provider-1" "{
  \"resourceType\": \"Practitioner\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"prac-provider-1\"}],
  \"name\": [{\"family\": \"Chen\", \"given\": [\"Sarah\"], \"prefix\": [\"Dr.\"]}]
}")

nurse_id=$(ensure_resource "Practitioner" "${IDENTIFIER_SYSTEM}|prac-nurse-1" "{
  \"resourceType\": \"Practitioner\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"prac-nurse-1\"}],
  \"name\": [{\"family\": \"Lopez\", \"given\": [\"Maria\"]}]
}")

ma_id=$(ensure_resource "Practitioner" "${IDENTIFIER_SYSTEM}|prac-ma-1" "{
  \"resourceType\": \"Practitioner\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"prac-ma-1\"}],
  \"name\": [{\"family\": \"Lee\", \"given\": [\"Jordan\"]}]
}")

provider_role_id=$(ensure_resource "PractitionerRole" "${IDENTIFIER_SYSTEM}|role-provider-1" "{
  \"resourceType\": \"PractitionerRole\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"role-provider-1\"}],
  \"practitioner\": {\"reference\": \"Practitioner/${provider_id}\"},
  \"organization\": {\"reference\": \"Organization/${org_id}\"},
  \"location\": [{\"reference\": \"Location/${location_id}\"}],
  \"code\": [{\"coding\": [{\"system\": \"http://terminology.hl7.org/CodeSystem/practitioner-role\", \"code\": \"doctor\", \"display\": \"Doctor\"}]}]
}")

nurse_role_id=$(ensure_resource "PractitionerRole" "${IDENTIFIER_SYSTEM}|role-nurse-1" "{
  \"resourceType\": \"PractitionerRole\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"role-nurse-1\"}],
  \"practitioner\": {\"reference\": \"Practitioner/${nurse_id}\"},
  \"organization\": {\"reference\": \"Organization/${org_id}\"},
  \"location\": [{\"reference\": \"Location/${location_id}\"}],
  \"code\": [{\"coding\": [{\"system\": \"http://terminology.hl7.org/CodeSystem/practitioner-role\", \"code\": \"nurse\", \"display\": \"Nurse\"}]}]
}")

ma_role_id=$(ensure_resource "PractitionerRole" "${IDENTIFIER_SYSTEM}|role-ma-1" "{
  \"resourceType\": \"PractitionerRole\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"role-ma-1\"}],
  \"practitioner\": {\"reference\": \"Practitioner/${ma_id}\"},
  \"organization\": {\"reference\": \"Organization/${org_id}\"},
  \"location\": [{\"reference\": \"Location/${location_id}\"}],
  \"code\": [{\"coding\": [{\"system\": \"http://terminology.hl7.org/CodeSystem/practitioner-role\", \"code\": \"assistant\", \"display\": \"Medical Assistant\"}]}]
}")

schedule_id=$(ensure_resource "Schedule" "${IDENTIFIER_SYSTEM}|schedule-provider-1" "{
  \"resourceType\": \"Schedule\",
  \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"schedule-provider-1\"}],
  \"active\": true,
  \"actor\": [
    {\"reference\": \"PractitionerRole/${provider_role_id}\", \"display\": \"Dr. Sarah Chen\"},
    {\"reference\": \"Location/${location_id}\", \"display\": \"Main Street Clinic - Suite 200\"}
  ]
}")

slot_times=("09:00" "09:30" "10:00" "10:30" "11:00" "11:30")
for offset in $(seq 0 "$DAYS_AHEAD"); do
  schedule_day="$(day_for_offset "$offset")"
  for slot_time in "${slot_times[@]}"; do
    slot_start=$(iso_time "${schedule_day}" "${slot_time}")
    slot_end=$(iso_time_plus_minutes "${schedule_day}" "${slot_time}" 30)
    slot_identifier="${IDENTIFIER_SYSTEM}|slot-${schedule_day}-${slot_time//:/}"
    ensure_resource "Slot" "${slot_identifier}" "{
      \"resourceType\": \"Slot\",
      \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"slot-${schedule_day}-${slot_time//:/}\"}],
      \"schedule\": {\"reference\": \"Schedule/${schedule_id}\"},
      \"status\": \"free\",
      \"start\": \"${slot_start}\",
      \"end\": \"${slot_end}\"
    }" >/dev/null
  done
done

patient_ids=()
while IFS= read -r pid; do
  if [ -n "$pid" ]; then
    patient_ids+=("$pid")
  fi
done < <(fhir_get "Patient?_count=6" | jq -r '.entry[]?.resource.id')

if [ "${#patient_ids[@]}" -lt 3 ]; then
  echo -e "${RED}Not enough patients found to seed appointments.${NC}"
  exit 1
fi

appointment_types=("Annual Wellness Visit" "Diabetes Follow-up" "Hypertension Check" "Care Gap Review" "Medication Review" "Preventive Screening")

for offset in $(seq 0 "$DAYS_AHEAD"); do
  schedule_day="$(day_for_offset "$offset")"
  for i in "${!slot_times[@]}"; do
    patient_index=$(( (offset * ${#slot_times[@]} + i) % ${#patient_ids[@]} ))
    patient_id="${patient_ids[$patient_index]}"
    if [ -z "$patient_id" ]; then
      continue
    fi
    slot_time="${slot_times[$i]}"
    start_time=$(iso_time "${schedule_day}" "${slot_time}")
    end_time=$(iso_time_plus_minutes "${schedule_day}" "${slot_time}" 30)
    appt_type="${appointment_types[$i]}"

    appointment_id=$(uuid_for "appointment-${schedule_day}-${slot_time}")
    if should_seed "appointment-task"; then
      appt_identifier="${IDENTIFIER_SYSTEM}|appt-${schedule_day}-${slot_time//:/}"
      existing_appt_id=$(get_id_by_identifier "Appointment" "${appt_identifier}")
      if [ -n "$existing_appt_id" ]; then
        appointment_id="$existing_appt_id"
      elif fhir_exists "Appointment" "$appointment_id"; then
        # Reruns can hit duplicate ID errors when identifier lookup is stale; reuse deterministic ID.
        :
      else
        appointment_id=$(fhir_post "Appointment" "{
          \"resourceType\": \"Appointment\",
          \"id\": \"${appointment_id}\",
          \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"appt-${schedule_day}-${slot_time//:/}\"}],
          \"status\": \"booked\",
          \"description\": \"${appt_type}\",
          \"start\": \"${start_time}\",
          \"end\": \"${end_time}\",
          \"appointmentType\": {\"text\": \"${appt_type}\"},
          \"participant\": [
            {\"actor\": {\"reference\": \"Patient/${patient_id}\"}, \"status\": \"accepted\"},
            {\"actor\": {\"reference\": \"PractitionerRole/${provider_role_id}\", \"display\": \"Dr. Sarah Chen\"}, \"status\": \"accepted\"},
            {\"actor\": {\"reference\": \"Location/${location_id}\", \"display\": \"Main Street Clinic - Suite 200\"}, \"status\": \"accepted\"}
          ]
        }" | jq -r '.id // empty' 2>/dev/null || true)
      fi

      ma_task_identifier="${IDENTIFIER_SYSTEM}|task-ma-${schedule_day}-${slot_time//:/}"
      existing_ma_task_id=$(get_id_by_identifier "Task" "${ma_task_identifier}")
      if [ -z "$existing_ma_task_id" ]; then
        ma_task_id=$(uuid_for "task-ma-${schedule_day}-${slot_time}")
        fhir_post "Task" "{
          \"resourceType\": \"Task\",
          \"id\": \"${ma_task_id}\",
          \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"task-ma-${schedule_day}-${slot_time//:/}\"}],
          \"status\": \"requested\",
          \"intent\": \"order\",
          \"priority\": \"routine\",
          \"code\": {\"text\": \"Check-in\"},
          \"for\": {\"reference\": \"Patient/${patient_id}\"},
          \"owner\": {\"reference\": \"PractitionerRole/${ma_role_id}\", \"display\": \"Jordan Lee (MA)\"},
          \"focus\": {\"reference\": \"Appointment/${appointment_id}\"},
          \"authoredOn\": \"${start_time}\",
          \"executionPeriod\": {\"start\": \"${start_time}\", \"end\": \"${start_time}\"}
        }" >/dev/null
      fi

      nurse_task_identifier="${IDENTIFIER_SYSTEM}|task-nurse-${schedule_day}-${slot_time//:/}"
      existing_nurse_task_id=$(get_id_by_identifier "Task" "${nurse_task_identifier}")
      if [ -z "$existing_nurse_task_id" ]; then
        nurse_task_id=$(uuid_for "task-nurse-${schedule_day}-${slot_time}")
        fhir_post "Task" "{
          \"resourceType\": \"Task\",
          \"id\": \"${nurse_task_id}\",
          \"identifier\": [{\"system\": \"${IDENTIFIER_SYSTEM}\", \"value\": \"task-nurse-${schedule_day}-${slot_time//:/}\"}],
          \"status\": \"requested\",
          \"intent\": \"order\",
          \"priority\": \"routine\",
          \"code\": {\"text\": \"Vitals\"},
          \"for\": {\"reference\": \"Patient/${patient_id}\"},
          \"owner\": {\"reference\": \"PractitionerRole/${nurse_role_id}\", \"display\": \"Maria Lopez (NP)\"},
          \"focus\": {\"reference\": \"Appointment/${appointment_id}\"},
          \"authoredOn\": \"${start_time}\",
          \"executionPeriod\": {\"start\": \"${start_time}\", \"end\": \"${start_time}\"}
        }" >/dev/null
      fi
    fi

    if should_seed "encounter"; then
      encounter_id=$(uuid_for "encounter-${schedule_day}-${slot_time}")
      if ! fhir_exists "Encounter" "$encounter_id"; then
        fhir_post "Encounter" "{
          \"resourceType\": \"Encounter\",
          \"id\": \"${encounter_id}\",
          \"status\": \"planned\",
          \"class\": {\"code\": \"AMB\", \"display\": \"Ambulatory\"},
          \"type\": [{\"text\": \"${appt_type}\"}],
          \"subject\": {\"reference\": \"Patient/${patient_id}\"},
          \"period\": {\"start\": \"${start_time}\", \"end\": \"${end_time}\"},
          \"participant\": [
            {\"individual\": {\"reference\": \"PractitionerRole/${provider_role_id}\", \"display\": \"Dr. Sarah Chen\"}}
          ],
          \"location\": [
            {\"location\": {\"reference\": \"Location/${location_id}\"}}
          ]
        }" >/dev/null
      fi
    fi
  done
done

echo -e "${GREEN}✓ Scheduling data seeded successfully.${NC}"
