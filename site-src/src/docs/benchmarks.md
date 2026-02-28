---
seo_title: "MicroRaft Benchmarks and Performance Notes for Java Raft"
description: "Review MicroRaft benchmarks, JMH setup, and performance notes before making claims about Java Raft throughput or latency."
keywords: "microraft benchmarks, java raft benchmarks, raft performance java, microraft jmh, raft throughput java"
schema_type: TechArticle
og_type: article
---
<div class="mr-doc-shell" data-mr-doc-layout="reference">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Benchmarks</h1>
    <p class="mr-page-summary">
      MicroRaft includes a JMH benchmark suite for the core module. Use it to evaluate
      Java Raft throughput and latency tradeoffs under a controlled setup, not to make
      context-free production claims.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Run the benchmark suite</h3>
      <pre class="mr-code-block"><code>./gradlew benchmark -Pmicroraft.javaVersion=21</code></pre>
      <p>If your machine uses a different installed JDK, replace <code>21</code> with that version.</p>
    </article>
    <article class="mr-doc-card">
      <h3>What these Java Raft benchmarks are for</h3>
      <ul class="mr-doc-list">
        <li>detecting regressions before they reach production workloads</li>
        <li>comparing implementation changes inside the same controlled environment</li>
        <li>measuring Java Raft throughput and latency tradeoffs under an explicit scenario</li>
      </ul>
    </article>
    <article class="mr-doc-card">
      <h3>What these numbers do not prove</h3>
      <ul class="mr-doc-list">
        <li>they do not describe every Java Raft deployment or every control-plane workload</li>
        <li>they are not independent of storage, transport, command shape, CPU, memory, or network assumptions</li>
        <li>they do not replace workload-specific soak tests, failure tests, or rollout rehearsals</li>
      </ul>
    </article>
    <article class="mr-doc-card">
      <h3>Publish benchmark results responsibly</h3>
      <p>When sharing performance claims, include:</p>
      <ul class="mr-doc-list">
        <li>the exact benchmark command and git revision</li>
        <li>JDK version and GC/runtime settings</li>
        <li>hardware profile and machine isolation assumptions</li>
        <li>scenario details, workload shape, and storage/network setup</li>
      </ul>
    </article>
    <article class="mr-doc-card">
      <h3>How to read the output</h3>
      <p>Look for trend changes first. If a benchmark is slower, ask whether the change bought safer durability, better batching, or more predictable failure behavior before treating it as a regression.</p>
    </article>
    <article class="mr-doc-card">
      <h3>Where to go next</h3>
      <ul class="mr-doc-list">
        <li><a href="/docs/use-cases/">Use Cases</a> if you want to map benchmark numbers to real workloads</li>
        <li><a href="/docs/production-checklist/">Production Checklist</a> if you want to validate rollout assumptions</li>
        <li><a href="/docs/resiliency-and-fault-tolerance/">Resiliency and Fault Tolerance</a> if you care more about failure behavior than peak throughput</li>
      </ul>
      <span class="mr-card-cta">Open guide</span>
    </article>
  </section>
</div>
