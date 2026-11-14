#!/usr/bin/env bash
# Smoke test: is the whole system alive and wired together?
# Usage: ./scripts/smoke-test.sh [gateway-url]   (default http://localhost:8080)
set -euo pipefail

BASE="${1:-http://localhost:8080}"
failures=0

check() {   # check <name> <expected status> <method> <url> [json body]
  local name=$1 expected=$2 method=$3 url=$4 body=${5:-}
  local code
  if [ -n "$body" ]; then
    code=$(curl -s -o /dev/null -w '%{http_code}' -X "$method" -H 'Content-Type: application/json' -d "$body" "$url")
  else
    code=$(curl -s -o /dev/null -w '%{http_code}' -X "$method" "$url")
  fi
  if [ "$code" = "$expected" ]; then
    echo "PASS  $name (HTTP $code)"
  else
    echo "FAIL  $name (expected $expected, got $code)"
    failures=$((failures + 1))
  fi
}

check "gateway health"            200 GET  "$BASE/actuator/health"
check "restaurant list"           200 GET  "$BASE/api/restaurants"
check "restaurant not found"      404 GET  "$BASE/api/restaurants/999999"
check "internal endpoint hidden"  404 GET  "$BASE/api/menu-items/1"
check "order list"                200 GET  "$BASE/api/orders?customerName=smoke"
check "place order end to end"    201 POST "$BASE/api/orders" \
  '{"customerName":"smoke","deliveryAddress":"Test street","items":[{"menuItemId":1,"quantity":1}]}'
check "empty order rejected"      400 POST "$BASE/api/orders" \
  '{"customerName":"smoke","deliveryAddress":"Test street","items":[]}'

if [ "$failures" -gt 0 ]; then
  echo "$failures check(s) failed"
  exit 1
fi
echo "All checks passed"
