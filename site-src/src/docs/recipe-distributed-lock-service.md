---
seo_title: "Build a Distributed Lock Service with MicroRaft in Java"
description: "Use MicroRaft to build a Java distributed lock service with strong consistency, explicit ownership, and failure-aware lock semantics."
keywords: "distributed lock service java, raft lock service, microraft lock service, fencing token raft, java raft use case"
schema_type: TechArticle
og_type: article
doc_layout: reference
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Recipe: Distributed Lock Service</h1>
    <p class="mr-page-summary">
      MicroRaft can back a Java distributed lock service when lock ownership,
      fencing, and failover semantics must stay strongly consistent under partition and recovery.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Typical lock-service state machine</h3>
      <ul class="mr-doc-list">
        <li>lock create, acquire, renew, and release commands</li>
        <li>owner identity, fencing token, and expiry metadata</li>
        <li>wait queues or lock-scoped sequencing state</li>
        <li>explicit revocation or timeout transitions</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Why Raft fits lock ownership</h3>
      <ul class="mr-doc-list">
        <li>lock services depend on single-writer semantics, not best-effort coordination</li>
        <li>fencing tokens map naturally to ordered committed log entries</li>
        <li>leadership and quorum rules support safe ownership transitions after failure</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Design rules that matter</h3>
      <ul class="mr-doc-list">
        <li>prefer fencing-token designs over simple boolean locks</li>
        <li>define lease expiry and retry behavior before exposing the API</li>
        <li>treat duplicate client retries as part of the contract, not an edge case</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Related reading</h3>
      <ul class="mr-doc-list">
        <li><a href="/docs/resiliency-and-fault-tolerance/">Resiliency and Fault Tolerance</a></li>
        <li><a href="/docs/production-checklist/">Production Checklist</a></li>
        <li><a href="/docs/recipe-coordination-service/">Recipe: Coordination Service</a></li>
      </ul>
      <span class="mr-card-cta">Open guide</span>
    </article>
  </section>
</div>
