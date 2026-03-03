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
```

## Directory Layout

- [`mkdocs.yml`](/Users/fatmakhv/Desktop/MicroRaft/site-src/mkdocs.yml): navigation, theme, plugins, and site metadata
- [`src/`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src): markdown content, demo page, docs pages, blog posts, assets
- [`src/stylesheets/microraft.css`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/stylesheets/microraft.css): shared visual system and layout rules
- [`src/javascripts/demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js): site behavior, demo behavior, tabs, footer injection
- [`overrides/`](/Users/fatmakhv/Desktop/MicroRaft/site-src/overrides): theme overrides such as extra meta tags
- [`scripts/check-seo.sh`](/Users/fatmakhv/Desktop/MicroRaft/site-src/scripts/check-seo.sh): basic site validation used by CI

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

That split is currently controlled by hardcoded path lists in [`demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js).

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
- custom site footer injection

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

## Review Findings

1. High: mobile navigation state tracking is wired to old Bootstrap class names, so the custom mobile-nav open state can silently fail.
   [`demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js#L211) looks for `.navbar-toggle` and the `in` class, while the generated MkDocs markup uses Bootstrap 5 style `navbar-toggler` and `collapse` state classes. The result is that `mr-mobile-nav-open` can miss real open/close transitions.

2. Medium: the site injects a custom footer in JavaScript while MkDocs still renders its own default footer.
   The custom footer is appended in [`demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js#L1091), but MkDocs also renders footer content from [`mkdocs.yml`](/Users/fatmakhv/Desktop/MicroRaft/site-src/mkdocs.yml#L5). If the default footer is not intentionally hidden, users can end up with two footer systems to maintain and style.

3. Medium: docs layout behavior is path-allowlist driven, so adding new docs pages can silently pick the wrong layout and TOC rule.
   [`demo.js`](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/javascripts/demo.js#L78) hardcodes which paths are `article` and which are `reference`. New docs pages will fall through to the default behavior unless that list is updated as part of the content change.

## Suggested Follow-ups

1. Move docs layout selection from hardcoded path lists into page metadata.
2. Replace the current mobile-menu state detection with Bootstrap 5 compatible events or class checks.
3. Decide whether the MkDocs default footer should be removed or the custom footer should replace it at the template level instead of at runtime.
