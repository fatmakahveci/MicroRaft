# MicroRaft Website Maintenance Guide

This directory contains the sources for `microraft.io`.

The site is built with MkDocs and then customized with a small set of local
overrides, page-specific markdown shells, one shared JavaScript file, and one
shared stylesheet.

## Local Development

Run from [`site-src`](/Users/fatmakhv/Desktop/MicroRaft/site-src):

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
./.venv/bin/mkdocs serve
```

Static build:

```bash
./.venv/bin/mkdocs build
```

Basic validation after a build:

```bash
./scripts/check-seo.sh site
./scripts/check-site-structure.sh site
```

## Directory Layout

- [`mkdocs.yml`](/Users/fatmakhv/Desktop/MicroRaft/site-src/mkdocs.yml): navigation, theme, plugins, and site metadata
- [`src/`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src): markdown content, demo page, docs pages, blog posts, assets
- [`src/stylesheets/microraft.css`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/stylesheets/microraft.css): shared visual system and layout rules
- [`src/javascripts/demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js): site behavior, demo behavior, and homepage/docs enhancements
- [`overrides/`](/Users/fatmakhv/Desktop/MicroRaft/site-src/overrides): theme overrides such as extra meta tags
- [`scripts/check-seo.sh`](/Users/fatmakhv/Desktop/MicroRaft/site-src/scripts/check-seo.sh): basic site validation used by CI
- [`scripts/check-site-structure.sh`](/Users/fatmakhv/Desktop/MicroRaft/site-src/scripts/check-site-structure.sh): homepage/footer/layout assertions for built output

## Page Model

There are four main page families:

1. Home
2. Demo
3. Docs
4. Blog

The current family is inferred in [`demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js) by looking for root page markers such as `.mr-home`, `.mr-blog-shell`, and `#mr-demo`.

Docs pages are further split into two layout types:

- `article`: long-form pages with narrower reading width
- `reference`: card-heavy or scan-heavy pages with wider layout

That split is declared per page with the `doc_layout` frontmatter field and then applied by
[`demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js).

## Styling Conventions

- Shared components should reuse existing classes before new ones are added.
- Homepage and docs cards prefer full-surface clickable links instead of a card plus a nested CTA button.
- External links can use small Font Awesome icons where it improves scanning.
- Demo styling should stay instructional first; avoid decorative motion that hides state changes.

## Interactive Behavior

[`demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js) currently owns:

- docs layout selection
- duplicate title cleanup for docs shells
- TOC visibility heuristics
- sticky header and nav enhancements
- blog reading-time and scroll progress
- the interactive Raft demo
- homepage tabs
- docs polish that depends on generated MkDocs markup

If this file keeps growing, it should be split by concern, for example:

- `layout.js`
- `nav.js`
- `reading.js`
- `demo.js`
- `footer.js`

## Content Conventions

- Internal doc links should stay in markdown form when possible.
- Raw HTML wrappers are acceptable for layout shells, but inner content should prefer normal HTML or normal markdown, not fragile mixed markdown-in-HTML patterns.
- For long-form article pages, use short summary bands or snippet notes ahead of dense code sections.

## Current Maintenance Notes

1. Footer ownership is in the template override layer.
   [`overrides/main.html`](/Users/fatmakhv/Desktop/MicroRaft/site-src/overrides/main.html) replaces the theme footer block, so footer changes should happen there first.

2. Docs layout selection now depends on frontmatter discipline.
   When a new docs page needs the wider card-heavy layout, set `doc_layout: reference`. Otherwise it will fall back to heuristic article/reference detection.

3. TOC visibility is still heuristic.
   [`demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js) decides whether the TOC should be visible based on heading count and content length, so long docs pages should be spot-checked after content edits.

## Suggested Follow-ups

1. Add a dedicated frontmatter field for TOC visibility so long pages do not depend on heuristics alone.
2. Split `demo.js` into smaller files once another site behavior change lands.
3. Do a browser-based mobile QA pass after any nav or layout change that touches MkDocs-generated markup.
