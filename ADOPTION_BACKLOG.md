# MicroRaft Adoption Backlog

This document turns the adoption and usability ideas into concrete repository tasks.
It is intentionally biased toward changes that can be shipped from this repository
without waiting for a larger product redesign.

## 1. Highest-Leverage Tasks

### 1. Quick Start That Works in 5 Minutes

Goal: let a new user run one command, observe leader election and operation commits,
and know where to go next.

Tasks:

- Rewrite the "Get started" section in [README.md](/Users/fatmakhv/Desktop/MicroRaft/README.md) to use a single verified local command path.
- Add a short "What you will see" subsection with 3-4 expected log lines or outcomes.
- Link directly from [README.md](/Users/fatmakhv/Desktop/MicroRaft/README.md) to [microraft-tutorial/README.md](/Users/fatmakhv/Desktop/MicroRaft/microraft-tutorial/README.md).
- Expand [microraft-tutorial/README.md](/Users/fatmakhv/Desktop/MicroRaft/microraft-tutorial/README.md) from dependency coordinates into an actual runnable tutorial entry point.

Acceptance criteria:

- A first-time user can copy one command from the root README and run a local example.
- The next step after the example is explicit.

### 2. "Why MicroRaft?" Positioning Page

Goal: help technical evaluators decide if MicroRaft fits their problem.

Tasks:

- Add a new "Why MicroRaft?" page under [site-src/src](/Users/fatmakhv/Desktop/MicroRaft/site-src/src).
- Explain where MicroRaft fits: library, not a turnkey database or lock service.
- Compare strengths against common alternatives at a high level: lightweight embeddable library, modular abstractions, production-oriented Raft features.
- Add "When not to use MicroRaft" to reduce wrong adoption.

Acceptance criteria:

- A reader can answer "why this project exists" and "is this the right abstraction level for me?" within one page.

### 3. Documentation Information Architecture Cleanup

Goal: reduce time-to-understanding for new users.

Tasks:

- Reorganize the home page in [site-src/src/index.md](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/index.md) around user intent:
  "What is it?", "Quick Start", "Core concepts", "Use cases", "Production concerns".
- Ensure root [README.md](/Users/fatmakhv/Desktop/MicroRaft/README.md) and website home use the same current version numbers and build tool guidance.
- Add a short "Start here" navigation block to both the repo README and website home page.

Acceptance criteria:

- Repo README and website stop feeling like two different entry points.
- A new visitor can choose between "run demo", "embed library", and "learn concepts".

## 2. Repo Tasks That Can Ship This Week

### 4. Turn Tutorial Module Into a Real Demo

Goal: demonstrate end-to-end value, not just artifact coordinates.

Tasks:

- Add a runnable scenario to [microraft-tutorial](/Users/fatmakhv/Desktop/MicroRaft/microraft-tutorial) that starts a small local cluster.
- Document the exact class or Gradle task to run in [microraft-tutorial/README.md](/Users/fatmakhv/Desktop/MicroRaft/microraft-tutorial/README.md).
- Print a compact success path in logs: leader elected, operation replicated, state observed.

Acceptance criteria:

- The tutorial module is useful even if the user has never worked with Raft before.

### 5. Add Troubleshooting for the First Run

Goal: prevent early drop-off when the first attempt fails.

Tasks:

- Create a troubleshooting page under [site-src/src](/Users/fatmakhv/Desktop/MicroRaft/site-src/src) for common first-run problems.
- Cover at least: wrong Java version, build tool mismatch, no leader elected locally, example command not found.
- Link troubleshooting from [README.md](/Users/fatmakhv/Desktop/MicroRaft/README.md) and the tutorial README.

Acceptance criteria:

- A user can self-serve the most common setup failures without opening an issue first.

### 6. Improve Contributor Onboarding

Goal: make the first contribution easier and less ambiguous.

Tasks:

- Expand [CONTRIBUTING.md](/Users/fatmakhv/Desktop/MicroRaft/CONTRIBUTING.md) with setup, local verification, module map, and PR expectations.
- Add a "Good first contributions" section with examples such as docs, examples, tests, and diagnostics.
- Add GitHub issue templates under `.github/ISSUE_TEMPLATE/` for bug reports and feature requests.

Acceptance criteria:

- A new contributor can understand how to validate a change locally without reading the Gradle files.

### 7. Show Production Readiness Signals

Goal: reduce perceived risk for adopters.

Tasks:

- Add a short observability guide covering metrics, logging, and health signals.
- Link to [microraft-metrics](/Users/fatmakhv/Desktop/MicroRaft/microraft-metrics) from the root README and website.
- Add a "Production checklist" page with persistence, monitoring, quorum-loss handling, and upgrade considerations.

Acceptance criteria:

- Readers can see how to operate MicroRaft, not just embed it.

## 3. Website and Adoption Work

### 8. Publish Use-Case Recipes

Goal: help users map MicroRaft to concrete systems they already understand.

Tasks:

- Add short recipe pages for metadata store, distributed lock service, and coordination service.
- For each recipe, state the state machine shape, persistence needs, and failure semantics at a high level.
- Link each recipe from the home page and README.

Acceptance criteria:

- Potential adopters can quickly see a familiar target architecture.

### 9. Add Benchmark and Performance Narrative

Goal: give evaluators confidence without forcing them to read code first.

Tasks:

- Document how to run the benchmark task already referenced in [README.md](/Users/fatmakhv/Desktop/MicroRaft/README.md).
- Publish at least one benchmark note explaining what is being measured and what is not.
- Avoid vague "fast" claims; prefer reproducible commands and environment notes.

Acceptance criteria:

- Performance claims become inspectable and credible.

### 10. Tighten Message Consistency Across Entry Points

Goal: remove confusion caused by stale or conflicting onboarding text.

Tasks:

- Audit [README.md](/Users/fatmakhv/Desktop/MicroRaft/README.md), [site-src/src/index.md](/Users/fatmakhv/Desktop/MicroRaft/site-src/src/index.md), and [microraft-tutorial/README.md](/Users/fatmakhv/Desktop/MicroRaft/microraft-tutorial/README.md) for mismatched versions, commands, and wording.
- Normalize terms such as "tutorial", "user guide", "demo", and "get started".
- Ensure the website and repository describe the same build flow.

Acceptance criteria:

- There is one clear story for beginners instead of multiple partial ones.

## Suggested Execution Order

1. Quick Start rewrite
2. Tutorial module as a real demo
3. Documentation information architecture cleanup
4. Troubleshooting page
5. Why MicroRaft? page
6. Contributor onboarding improvements
7. Production readiness and observability docs
8. Use-case recipes
9. Benchmark narrative
10. Final consistency pass across README and website

## Notes From Current Repository State

- The root README is already stronger than the website home page and should become the canonical onboarding source.
- The website home page still contains older onboarding details and should be updated together with the README.
- The tutorial README currently looks like a dependency page, not a tutorial.
- Contributor guidance exists, but it is still too thin for a first-time external contributor.
