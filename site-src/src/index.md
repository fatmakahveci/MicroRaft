---
seo_title: "MicroRaft Java Raft Library for Consensus, Metadata Stores, and Coordination"
description: "MicroRaft is an embeddable Java Raft library for metadata stores, distributed locks, leader election, and control-plane coordination services."
keywords: "MicroRaft, Java Raft library, embeddable Raft Java, Java consensus library, metadata store Java, distributed lock Java, control plane coordination"
schema_type: SoftwareApplication
og_type: website
---
<div class="mr-home">
  <section class="mr-hero">
    <div class="mr-hero-grid">
      <div>
        <span class="mr-page-kicker mr-eyebrow">Embeddable Java Raft</span>
        <h1 class="mr-page-title">Java Raft for metadata and coordination.</h1>
        <p class="mr-page-summary">
          Embed a CP core inside your Java service when metadata, coordination,
          or lock state must stay strongly consistent.
        </p>
        <ul class="mr-hero-list">
          <li>Embeddable library, not a turnkey data platform</li>
          <li>You keep control over transport, persistence, and failure behavior</li>
        </ul>
        <div class="mr-hero-actions">
          <a class="mr-button mr-button-primary" href="/demo/"><span class="fa-solid fa-play mr-button-icon" aria-hidden="true"></span><span>Open Interactive Demo</span></a>
          <a class="mr-button mr-button-secondary" href="/docs/documentation/"><span class="fa-solid fa-book-open mr-button-icon" aria-hidden="true"></span><span>Start Documentation</span></a>
        </div>
        <div class="mr-tab-actions">
          <a class="mr-button mr-button-ghost" href="/docs/faq/"><span class="fa-regular fa-circle-question mr-button-icon" aria-hidden="true"></span><span>Read the FAQ</span></a>
          <a class="mr-button mr-button-ghost" href="/docs/use-cases/"><span class="fa-regular fa-compass mr-button-icon" aria-hidden="true"></span><span>See Use Cases</span></a>
        </div>
      </div>
      <aside class="mr-hero-card">
        <div class="mr-hero-card-head">
          <span class="mr-hero-card-kicker">Fastest path to first signal</span>
          <h3>Quick Start</h3>
          <p>Run one local test to see leader election, quorum, and commit behavior from the repository root.</p>
        </div>
        <div class="mr-hero-command-panel">
          <div class="mr-hero-command-row">
            <span class="mr-hero-command-label">Command</span>
            <span class="mr-hero-command-value">Gradle test target</span>
          </div>
          <pre class="mr-code-block"><code>./gradlew :microraft-tutorial:test \
  --tests io.microraft.tutorial.OperationCommitTest</code></pre>
          <div class="mr-hero-command-notes">
            <span class="mr-hero-card-chip">Java 11 build target</span>
            <span class="mr-hero-card-chip">3-node local tutorial</span>
          </div>
        </div>
        <div class="mr-hero-card-meta">
          <span class="mr-hero-card-chip">Quorum + commit flow</span>
        </div>
      </aside>
    </div>
  </section>

  <section>
    <h2 class="mr-section-heading">Start here</h2>
    <p class="mr-muted">Use this order if you are seeing MicroRaft for the first time.</p>
    <div class="mr-steps-grid">
      <a class="mr-step mr-home-link-card" href="/docs/setup/">
        <span class="mr-step-index">1</span>
        <h3>Run a local test</h3>
        <p>Verify the local 3-node flow first.</p>
        <span class="mr-card-cta">Open guide</span>
      </a>
      <a class="mr-step mr-home-link-card" href="/docs/main-abstractions/">
        <span class="mr-step-index">2</span>
        <h3>Learn the abstractions</h3>
        <p>Move to Main Abstractions and the atomic register tutorial.</p>
        <span class="mr-card-cta">Open guide</span>
      </a>
      <a class="mr-step mr-home-link-card" href="/docs/production-checklist/">
        <span class="mr-step-index">3</span>
        <h3>Check rollout readiness</h3>
        <p>Use monitoring, troubleshooting, and rollout guidance before production.</p>
        <span class="mr-card-cta">Open checklist</span>
      </a>
    </div>
  </section>

  <section>
    <h2 class="mr-section-heading">What you can build</h2>
    <p class="mr-muted">These are the clearest fits for an embeddable Java Raft core.</p>
    <div class="mr-card-grid">
      <a class="mr-card mr-home-link-card" href="/docs/recipe-metadata-store/">
        <h3>Metadata stores</h3>
        <p>Keep shard maps, ownership records, and cluster metadata strongly consistent.</p>
        <span class="mr-card-cta">View recipe</span>
      </a>
      <a class="mr-card mr-home-link-card" href="/docs/recipe-coordination-service/">
        <h3>Coordination services</h3>
        <p>Drive control-plane decisions, registrations, elections, and state transitions with ordered commits.</p>
        <span class="mr-card-cta">View recipe</span>
      </a>
      <a class="mr-card mr-home-link-card" href="/docs/recipe-distributed-lock-service/">
        <h3>Distributed locks</h3>
        <p>Build fencing-token-based lock services with explicit consistency and failure semantics.</p>
        <span class="mr-card-cta">View recipe</span>
      </a>
    </div>
  </section>

  <section class="mr-home-band">
    <div class="mr-home-band-grid">
      <article>
        <h2 class="mr-section-heading">Use MicroRaft when</h2>
        <ul class="mr-home-list">
          <li>you need a CP core inside a Java service</li>
          <li>you want to own transport and persistence</li>
          <li>you care about explicit failure semantics</li>
        </ul>
      </article>
    </div>
  </section>

  <section class="mr-callout">
    <h2 class="mr-section-heading">Choose your next read</h2>
    <p class="mr-muted">
      Move from fit to rollout only when you need that level of detail.
    </p>
    <div class="mr-card-grid">
      <a class="mr-card mr-home-link-card" href="/docs/why-microraft/">
        <h3>Decide if it fits</h3>
        <p>Read the positioning, tradeoffs, and use-case guidance before you commit to an embeddable CP core.</p>
        <span class="mr-card-cta">Open guide</span>
      </a>
      <a class="mr-card mr-home-link-card" href="/docs/faq/">
        <h3>Read the FAQ</h3>
        <p>Get short answers on fit, scope, and what MicroRaft is not.</p>
        <span class="mr-card-cta">Open FAQ</span>
      </a>
      <a class="mr-card mr-home-link-card" href="/docs/production-checklist/">
        <h3>Open the production checklist</h3>
        <p>Validate persistence, observability, transport, and failure handling before rollout.</p>
        <span class="mr-card-cta">Open checklist</span>
      </a>
      <a class="mr-card mr-home-link-card" href="https://github.com/MicroRaft/MicroRaft">
        <h3>Browse source and Javadoc</h3>
        <p>Go straight to GitHub and Javadoc when you want implementation detail instead of onboarding guidance.</p>
        <span class="mr-card-cta mr-card-cta-github"><span class="fa-brands fa-github" aria-hidden="true"></span><span>Open source and API</span></span>
      </a>
    </div>
  </section>
</div>
