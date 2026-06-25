#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

for forbidden in \
  "node_modules" "dist" ".angular" ".omx" ".idea" ".scannerwork" \
  "hybris/bin/platform" "hybris/data" "hybris/log" "hybris/temp" "output"
do
  if find "$root" -path "*/$forbidden" -o -path "*/$forbidden/*" | grep -q .; then
    echo "Forbidden path found: $forbidden" >&2
    exit 1
  fi
done

if find "$root" -name '*.jar' -o -name '*.class' -o -name '*.war' | grep -q .; then
  echo "Forbidden binary build artifact found" >&2
  exit 1
fi

if command -v rg >/dev/null 2>&1; then
  if rg -n --hidden --glob '!assets/stripe-sap-commerce-payment.gif' --glob '!**/package-lock.json' --glob '!scripts/check_release_hygiene.sh' \
    '(sk_live_[A-Za-z0-9]{16,}|sk_test_[A-Za-z0-9]{16,}|pk_live_[A-Za-z0-9]{16,}|pk_test_[A-Za-z0-9]{16,}|rk_live_[A-Za-z0-9]{16,}|rk_test_[A-Za-z0-9]{16,}|whsec_[A-Za-z0-9]{16,})' "$root"; then
    echo "Potential Stripe secret found" >&2
    exit 1
  fi
  if rg -n --hidden --glob '!**/scripts/check_release_hygiene.sh' \
    'confidential and proprietary information|Copyright \(c\) 20[0-9]{2} SAP SE' "$root"; then
    echo "Incompatible SAP proprietary header found" >&2
    exit 1
  fi
fi

echo "Release hygiene check passed: $root"
