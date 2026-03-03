---
seo_title: "MicroRaft Use Cases for Java Metadata Stores, Locks, and Coordination"
description: "Explore MicroRaft use cases for Java metadata stores, distributed locks, leader election services, and control-plane coordination."
keywords: "MicroRaft use cases, Java Raft use cases, distributed lock service Java, metadata store Raft, leader election service, coordination service Raft"
schema_type: CollectionPage
og_type: website
doc_layout: reference
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Use Cases</h1>
    <p class="mr-page-summary">
      Use this page when you want to map MicroRaft to a real Java system, not just
      read about the protocol. These are the workloads where an embeddable CP core
      is usually the right shape.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Metadata stores</h3>
      <p>Store ownership records, shard maps, and cluster configuration with strong consistency.</p>
      <ul>
        <li><a href="/docs/recipe-metadata-store/">Recipe: Metadata Store</a></li>
        <li><a href="/docs/production-checklist/">Production Checklist</a></li>
        <li><a href="/docs/benchmarks/">Benchmarks</a></li>
      </ul>
      <a class="mr-card-cta" href="/docs/recipe-metadata-store/">View recipe</a>
    </article>

    <article class="mr-doc-card">
      <h3>Distributed lock services</h3>
      <p>Build fencing-token based lock flows where stale owners must not keep control after failover.</p>
      <ul>
        <li><a href="/docs/recipe-distributed-lock-service/">Recipe: Distributed Lock Service</a></li>
        <li><a href="/docs/resiliency-and-fault-tolerance/">Resiliency and Fault Tolerance</a></li>
        <li><a href="/docs/troubleshooting/">Troubleshooting</a></li>
      </ul>
      <a class="mr-card-cta" href="/docs/recipe-distributed-lock-service/">View recipe</a>
    </article>

    <article class="mr-doc-card">
      <h3>Coordination services</h3>
      <p>Use ordered commits for elections, registrations, and control-plane transitions.</p>
      <ul>
        <li><a href="/docs/recipe-coordination-service/">Recipe: Coordination Service</a></li>
        <li><a href="/docs/monitoring/">Monitoring</a></li>
        <li><a href="/docs/configuration/">Configuration</a></li>
      </ul>
      <a class="mr-card-cta" href="/docs/recipe-coordination-service/">View recipe</a>
    </article>

    <article class="mr-doc-card">
      <h3>Evaluation path</h3>
      <p>If you are still deciding whether a Java Raft library is the right fit, start here.</p>
      <ol>
        <li><a href="/docs/why-microraft/">Why MicroRaft?</a></li>
        <li><a href="/demo/">Interactive Demo</a></li>
        <li><a href="/docs/setup/">Setup</a></li>
      </ol>
      <a class="mr-card-cta" href="/docs/why-microraft/">Read why MicroRaft</a>
    </article>
  </section>

  <section class="mr-doc-band">
    <h2>Decision rule</h2>
    <p>
      Reach for MicroRaft when you want to embed consensus into a Java service you
      already own. Pick a higher-level platform when you want a turnkey data product
      instead of a library integration surface.
    </p>
  </section>
</div>
