#!/usr/bin/env bash

set -euo pipefail

SITE_DIR_INPUT="${1:-site}"

if [[ ! -d "${SITE_DIR_INPUT}" ]]; then
  echo "Site directory not found: ${SITE_DIR_INPUT}" >&2
  exit 1
fi

SITE_DIR="$(cd "${SITE_DIR_INPUT}" && pwd -P)"

status=0

has_pattern() {
  local file="$1"
  local pattern="$2"
  local attempt
  local rc

  for attempt in 1 2 3; do
    if [[ ! -f "${file}" ]]; then
      sleep 0.1
      continue
    fi

    if grep -Fq -- "${pattern}" "${file}" 2>/dev/null; then
      return 0
    fi

    rc=$?
    if [[ ${rc} -eq 2 ]]; then
      sleep 0.1
      continue
    fi

    return 1
  done

  return 2
}

while IFS= read -r -d '' file; do
  if [[ "${file}" == *"/404.html" ]]; then
    continue
  fi
  for pattern in \
    '<title>' \
    '<meta name="description"' \
    '<link rel="canonical"' \
    '<meta property="og:title"' \
    '<meta property="og:description"' \
    '<meta property="og:image"' \
    '<meta name="twitter:title"' \
    '<meta name="twitter:description"' \
    '<script type="application/ld+json">'; do
    has_pattern "${file}" "${pattern}"
    rc=$?
    if [[ ${rc} -ne 0 ]]; then
      if [[ ${rc} -eq 2 ]]; then
        continue
      fi
      echo "Missing SEO tag ${pattern} in ${file}" >&2
      status=1
    fi
  done
done < <(find "${SITE_DIR}" -name '*.html' -print0)

if [[ ! -f "${SITE_DIR}/robots.txt" ]]; then
  echo "Missing robots.txt in ${SITE_DIR}" >&2
  status=1
fi

if [[ ! -f "${SITE_DIR}/sitemap.xml" ]]; then
  echo "Missing sitemap.xml in ${SITE_DIR}" >&2
  status=1
fi

while IFS= read -r -d '' file; do
  if grep -Eq 'TODO LINK HERE|TODO' "${file}"; then
    echo "Found TODO marker in built site output: ${file}" >&2
    status=1
  fi
done < <(find "${SITE_DIR}" \( -name '*.html' -o -name '*.xml' -o -name '*.txt' \) -print0)

if [[ ${status} -ne 0 ]]; then
  exit ${status}
fi

echo "Basic SEO checks passed for ${SITE_DIR}"
