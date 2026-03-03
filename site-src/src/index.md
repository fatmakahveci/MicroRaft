---
seo_title: "MicroRaft Java Raft Library for Consensus, Metadata Stores, and Coordination"
description: "MicroRaft is an embeddable Java Raft library for consensus, metadata stores, coordination services, distributed locks, and control-plane systems."
keywords: "java raft library, raft implementation java, embeddable raft java, java consensus library, microraft"
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
          Embed a CP core inside your own Java service. Do not use it when you
          want a turnkey distributed database or a managed coordination product.
        </p>
        <ul class="mr-hero-list">
          <li>Embeddable CP core, not a full external platform</li>
          <li>You keep control over transport, persistence, and failure behavior</li>
        </ul>
        <div class="mr-hero-actions">
          <a class="mr-button mr-button-primary" href="/demo/"><span class="fa fa-play mr-button-icon" aria-hidden="true"></span><span>Open Interactive Demo</span></a>
          <a class="mr-button mr-button-secondary" href="/docs/documentation/"><span class="fa fa-book mr-button-icon" aria-hidden="true"></span><span>Start Documentation</span></a>
        </div>
        <div class="mr-tab-actions">
          <a class="mr-button mr-button-ghost" href="/docs/faq/"><span class="fa fa-question-circle mr-button-icon" aria-hidden="true"></span><span>Read the FAQ</span></a>
          <a class="mr-button mr-button-ghost" href="/docs/use-cases/"><span class="fa fa-compass mr-button-icon" aria-hidden="true"></span><span>See Use Cases</span></a>
        </div>
      </div>
      <aside class="mr-hero-card">
        <h3>Quick Start</h3>
        <p>Run a 3-node local tutorial flow from the repository root.</p>
        <pre class="mr-code-block"><code>./gradlew :microraft-tutorial:test \
  --tests io.microraft.tutorial.OperationCommitTest</code></pre>
        <p class="mr-muted">MicroRaft builds against Java 11.</p>
      </aside>
    </div>
  </section>

  <section class="mr-proof-strip">
    <a class="mr-proof" href="https://central.sonatype.com/artifact/io.microraft/microraft"><span class="fa fa-cube mr-proof-icon" aria-hidden="true"></span><span>Maven Central</span></a>
    <a class="mr-proof" href="https://javadoc.io/doc/io.microraft/microraft"><span class="fa fa-code mr-proof-icon" aria-hidden="true"></span><span>Javadoc</span></a>
    <a class="mr-proof" href="https://github.com/MicroRaft/MicroRaft"><span class="fa fa-github mr-proof-icon" aria-hidden="true"></span><span>GitHub</span></a>
    <a class="mr-proof" href="/docs/production-checklist/"><span class="fa fa-shield mr-proof-icon" aria-hidden="true"></span><span>Production guidance</span></a>
  </section>

  <section class="mr-stat-grid">
    <div class="mr-stat">
      <strong>1 JAR</strong>
      <span>Lightweight embeddable core</span>
    </div>
    <div class="mr-stat">
      <strong>CP-first</strong>
      <span>For metadata, coordination, and control planes</span>
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
      <article>
        <h2 class="mr-section-heading">Do not use it when</h2>
        <ul class="mr-home-list">
          <li>you want a turnkey distributed database</li>
          <li>you want a managed coordination product</li>
          <li>you do not want to own state-machine behavior</li>
        </ul>
      </article>
    </div>
  </section>

  <section>
    <h2 class="mr-section-heading">Choose your entry point</h2>
    <div class="mr-home-tabs" data-mr-tabs>
      <div class="mr-tab-nav" role="tablist" aria-label="Homepage paths">
        <button class="mr-tab-button is-active" type="button" role="tab" aria-selected="true" data-tab-target="getting-started">
          Getting Started
        </button>
        <button class="mr-tab-button" type="button" role="tab" aria-selected="false" data-tab-target="evaluate">
          Evaluate
        </button>
        <button class="mr-tab-button" type="button" role="tab" aria-selected="false" data-tab-target="operate">
          Operate
        </button>
      </div>
      <div class="mr-tab-panel is-active" role="tabpanel" data-tab-panel="getting-started">
        <div class="mr-tab-panel-grid">
          <a class="mr-card mr-home-link-card" href="/docs/setup/">
            <h3>Run the shortest path first</h3>
            <p>Start with the local tutorial test. It is the fastest way to see leader election, quorum, and commit flow in one place.</p>
            <pre class="mr-code-block"><code>./gradlew :microraft-tutorial:test \
  --tests io.microraft.tutorial.OperationCommitTest</code></pre>
            <span class="mr-card-cta">Open guide</span>
          </a>
          <a class="mr-card mr-home-link-card" href="/docs/tutorial-building-an-atomic-register/">
            <h3>Turn it into a real state machine</h3>
            <p>Once the local cluster makes sense, move to the atomic register walkthrough and see the smallest practical integration shape.</p>
            <span class="mr-card-cta">Open tutorial</span>
          </a>
        </div>
      </div>
      <div class="mr-tab-panel" role="tabpanel" hidden data-tab-panel="evaluate">
        <div class="mr-tab-panel-grid">
          <a class="mr-card mr-home-link-card" href="/docs/why-microraft/">
            <h3>Check the abstraction fit</h3>
            <p>Read this first if you want to know where an embeddable Java Raft core fits, and where a turnkey data platform is the better choice.</p>
            <span class="mr-card-cta">Read guide</span>
          </a>
          <a class="mr-card mr-home-link-card" href="/docs/use-cases/">
            <h3>Map it to real workloads</h3>
            <p>Go here when you want to evaluate MicroRaft through metadata stores, lock services, and coordination systems instead of protocol theory.</p>
            <span class="mr-card-cta">View use cases</span>
          </a>
        </div>
      </div>
      <div class="mr-tab-panel" role="tabpanel" hidden data-tab-panel="operate">
        <div class="mr-tab-panel-grid">
          <a class="mr-card mr-home-link-card" href="/docs/production-checklist/">
            <h3>Open the rollout checklist</h3>
            <p>Use this once the local flow is clear and you need a practical checklist for observability, persistence, transport, and failure handling.</p>
            <span class="mr-card-cta">Open checklist</span>
          </a>
          <a class="mr-card mr-home-link-card" href="/docs/resiliency-and-fault-tolerance/">
            <h3>Validate failure behavior</h3>
            <p>Check follower loss, recovery catch-up, quorum behavior, and leader changes before you trust the cluster in production.</p>
            <span class="mr-card-cta">Open guide</span>
          </a>
        </div>
      </div>
    </div>
  </section>

  <section class="mr-callout">
    <h2 class="mr-section-heading">Why teams reach for MicroRaft</h2>
    <div class="mr-card-grid">
      <article>
        <h3>Embeddable by design</h3>
        <p>Bring Raft into your Java service instead of adopting a full external data platform.</p>
      </article>
      <article>
        <h3>Modular integration points</h3>
        <p>Keep control over persistence, networking, serialization, state machine logic, and threading decisions.</p>
      </article>
      <article>
        <h3>Operationally serious</h3>
        <p>Snapshotting, membership changes, quorum-aware queries, leadership transfer, and metrics support are already part of the story.</p>
      </article>
    </div>
  </section>

  <section>
    <h2 class="mr-section-heading">What you can build</h2>
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
        <p>Build fencing-token based lock services with explicit consistency and failure semantics.</p>
        <span class="mr-card-cta">View recipe</span>
      </a>
    </div>
  </section>

  <section>
    <h2 class="mr-section-heading">From demo to real code</h2>
    <div class="mr-card-grid">
      <a class="mr-card mr-home-link-card" href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/LeaderElectionTest.java">
        <h3>See the cluster form</h3>
        <p>The shortest runnable example is the leader election tutorial test.</p>
        <span class="mr-card-cta">View source</span>
      </a>
      <a class="mr-card mr-home-link-card" href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/OperationCommitTest.java">
        <h3>See replication happen</h3>
        <p>The operation commit scenario is the best bridge from the website demo to executable code.</p>
        <span class="mr-card-cta">View source</span>
      </a>
      <a class="mr-card mr-home-link-card" href="https://github.com/MicroRaft/MicroRaft/tree/master/microraft-tutorial/src/main/java/io/microraft/tutorial/atomicregister">
        <h3>See the sample state machine</h3>
        <p>The atomic register tutorial classes show the smallest practical integration shape.</p>
        <span class="mr-card-cta">View source</span>
      </a>
    </div>
  </section>

  <section>
    <h2 class="mr-section-heading">Read in this order</h2>
    <div class="mr-steps-grid">
      <a class="mr-step mr-home-link-card" href="/docs/setup/">
        <span class="mr-step-index">1</span>
        <h3>Run the tutorial</h3>
        <p>Start from Setup and verify the local 3-node example.</p>
        <span class="mr-card-cta">Open guide</span>
      </a>
      <a class="mr-step mr-home-link-card" href="/docs/main-abstractions/">
        <span class="mr-step-index">2</span>
        <h3>Learn the core APIs</h3>
        <p>Move to Main Abstractions and the atomic register tutorial.</p>
        <span class="mr-card-cta">Open guide</span>
      </a>
      <a class="mr-step mr-home-link-card" href="/docs/production-checklist/">
        <span class="mr-step-index">3</span>
        <h3>Harden for production</h3>
        <p>Use the monitoring, troubleshooting, and production checklist guides before rollout.</p>
        <span class="mr-card-cta">Open checklist</span>
      </a>
    </div>
  </section>

  <section class="mr-callout">
    <h2 class="mr-section-heading">Before you adopt</h2>
    <div class="mr-card-grid">
      <a class="mr-card mr-home-link-card" href="/docs/faq/">
        <h3>Read the FAQ</h3>
        <p>Get short answers on fit, scope, and what MicroRaft is not.</p>
        <span class="mr-card-cta">Read answers</span>
      </a>
      <a class="mr-card mr-home-link-card" href="/docs/use-cases/">
        <h3>Review use cases</h3>
        <p>Map MicroRaft to metadata stores, lock services, and coordination workloads.</p>
        <span class="mr-card-cta">View use cases</span>
      </a>
      <a class="mr-card mr-home-link-card" href="/docs/production-checklist/">
        <h3>Open the production checklist</h3>
        <p>Validate persistence, observability, transport, and failure handling before rollout.</p>
        <span class="mr-card-cta">Open checklist</span>
      </a>
    </div>
  </section>
</div>
