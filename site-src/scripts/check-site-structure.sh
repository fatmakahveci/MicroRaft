#!/usr/bin/env bash

set -euo pipefail

SITE_DIR="${1:-site}"

if [[ ! -d "${SITE_DIR}" ]]; then
  echo "Site directory not found: ${SITE_DIR}" >&2
  exit 1
fi

status=0

homepage="${SITE_DIR}/index.html"
if [[ ! -f "${homepage}" ]]; then
  echo "Homepage not found: ${homepage}" >&2
  exit 1
fi

for pattern in \
  'Choose your entry point' \
  'Why teams reach for MicroRaft' \
  'What you can build' \
  'From demo to real code' \
  'Read in this order' \
  'Before you adopt'; do
  if ! rg -Fq "${pattern}" "${homepage}"; then
    echo "Missing homepage section ${pattern} in ${homepage}" >&2
    status=1
  fi
done

for removed_pattern in \
  'Start in the right order' \
  'Common questions before adopting a Java Raft library' \
  '<footer class="mr-site-footer">'; do
  if rg -Fq "${removed_pattern}" "${homepage}"; then
    echo "Found stale homepage/footer markup ${removed_pattern} in ${homepage}" >&2
    status=1
  fi
done

if ! rg -Fq '<div class="mr-site-footer" role="contentinfo">' "${homepage}"; then
  echo "Missing footer contentinfo wrapper in ${homepage}" >&2
  status=1
fi

while IFS= read -r -d '' file; do
  if ! rg -Fq '<meta name="mr-doc-layout" content="' "${file}"; then
    echo "Missing doc layout metadata in ${file}" >&2
    status=1
  fi
done < <(find "${SITE_DIR}/docs" -name 'index.html' -print0)

if [[ ${status} -ne 0 ]]; then
  exit ${status}
fi

echo "Basic structure checks passed for ${SITE_DIR}"
