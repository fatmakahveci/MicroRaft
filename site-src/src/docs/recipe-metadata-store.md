---
seo_title: "Build a Metadata Store with MicroRaft in Java"
description: "See how MicroRaft fits a Java metadata store that needs strong consistency for ownership records, cluster maps, and control-plane state."
keywords: "metadata store raft, java metadata store raft, microraft metadata store, control plane metadata java, raft use case"
schema_type: TechArticle
og_type: article
---
<div class="mr-doc-shell" data-mr-doc-layout="reference">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Recipe: Metadata Store</h1>
    <p class="mr-page-summary">
      MicroRaft is a strong fit for a Java metadata store when ownership records,
      shard maps, and control-plane state must remain strongly consistent under failure.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Typical metadata state machine</h3>
      <ul class="mr-doc-list">
        <li>partition assignments and ownership transfers</li>
        <li>leader leases and coordinator epochs</li>
        <li>table, shard, or tenant metadata</li>
        <li>feature flags or routing records that must change atomically</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Why a Java Raft metadata store fits</h3>
      <ul class="mr-doc-list">
        <li>metadata workloads are usually write-light and consistency-heavy</li>
        <li>state is often compact enough for aggressive snapshots and fast recovery</li>
        <li>embedding Raft into the control plane keeps operational dependencies small</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Design rules that matter</h3>
      <ul class="mr-doc-list">
        <li>keep commands deterministic, explicit, and small</li>
        <li>treat metadata migrations and ownership changes as first-class commands</li>
        <li>choose query consistency per endpoint instead of one read mode everywhere</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Related reading</h3>
      <ul class="mr-doc-list">
        <li><a href="/docs/main-abstractions/">Main Abstractions</a></li>
        <li><a href="/docs/production-checklist/">Production Checklist</a></li>
        <li><a href="/docs/recipe-coordination-service/">Recipe: Coordination Service</a></li>
      </ul>
      <span class="mr-card-cta">Open guide</span>
    </article>
  </section>
</div>
