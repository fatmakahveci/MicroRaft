#!/usr/bin/env bash

set -euo pipefail

SITE_DIR="${1:-site}"

if [[ ! -d "${SITE_DIR}" ]]; then
  echo "Site directory not found: ${SITE_DIR}" >&2
  exit 1
fi

status=0

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
    if ! rg -Fq "${pattern}" "${file}"; then
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

if find "${SITE_DIR}" \( -name '*.html' -o -name '*.xml' -o -name '*.txt' \) -print0 | xargs -0 rg -n 'TODO LINK HERE|TODO' >/dev/null; then
  echo "Found TODO marker in built site output" >&2
  status=1
fi

if [[ ${status} -ne 0 ]]; then
  exit ${status}
fi

echo "Basic SEO checks passed for ${SITE_DIR}"
