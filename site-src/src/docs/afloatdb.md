---
seo_title: "AfloatDB: A Distributed Key-Value Store Built with MicroRaft"
description: "AfloatDB is a case study showing how a distributed key-value store can be built on top of MicroRaft."
keywords: "afloatdb microraft, distributed key value store raft, java raft case study, microraft example system, raft key value store"
schema_type: TechArticle
og_type: article
---
<div class="mr-doc-shell" data-mr-doc-layout="reference">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">AfloatDB</h1>
    <p class="mr-page-summary">
      AfloatDB is a small distributed key-value store built on top of MicroRaft.
      Treat it as a concrete case study for what an embedded Raft-powered service
      can look like in practice.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>What it shows</h3>
      <ul class="mr-doc-list">
        <li>a state machine implemented on top of MicroRaft</li>
        <li>Protocol Buffers models for replicated operations</li>
        <li>gRPC transport and service boundaries around the Raft core</li>
      </ul>
    </article>
    <article class="mr-doc-card">
      <h3>Why it matters</h3>
      <p>
        MicroRaft itself is intentionally modular. AfloatDB is useful because it
        shows one opinionated way to fill in transport, serialization, and
        storage-adjacent application logic.
      </p>
    </article>
    <article class="mr-doc-card">
      <h3>Use it as</h3>
      <ul class="mr-doc-list">
        <li>a reference when you need a concrete end-to-end example</li>
        <li>a source of integration ideas for your own service boundaries</li>
        <li>a reminder that MicroRaft is a library, not a turnkey database</li>
      </ul>
    </article>
    <article class="mr-doc-card">
      <h3>Repository</h3>
      <p>
        Browse the project on
        <a href="https://github.com/MicroRaft/AfloatDB" target="_blank" rel="noreferrer">GitHub</a>.
      </p>
      <span class="mr-card-cta">View source</span>
    </article>
  </section>
</div>
