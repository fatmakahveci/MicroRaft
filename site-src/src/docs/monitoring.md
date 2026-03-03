---
seo_title: "MicroRaft Monitoring for Java Raft Metrics, Logs, and Health Signals"
description: "Monitor MicroRaft with Raft metrics, node reports, logs, and operational signals that explain quorum, leadership, load, and recovery."
keywords: "MicroRaft monitoring, Java Raft metrics, RaftNodeReport, quorum metrics, leader health metrics, recovery observability"
schema_type: TechArticle
og_type: article
doc_layout: reference
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Monitoring</h1>
    <p class="mr-page-summary">
      Monitoring is how you turn Raft from a library into an operable subsystem.
      MicroRaft exposes both pull-style and push-style surfaces for this.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Pull-based reporting</h3>
      <p><code>RaftNodeReport</code> contains internal state such as role, term, leader, last log index, and commit index.</p>
      <ul>
        <li>query reports via <code>RaftNode.getReport()</code></li>
        <li>export them into your existing monitoring system</li>
        <li>use them for snapshots of current cluster health</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Push-based reporting</h3>
      <p><code>RaftNodeReportListener</code> is invoked when important state changes happen, such as leader change, term change, or snapshot installation.</p>
      <ul>
        <li>capture state transitions as they happen</li>
        <li>feed alerts and event streams promptly</li>
        <li>react to topology and health changes without polling lag</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Micrometer integration</h3>
      <p>MicroRaft offers a dedicated module for publishing metrics via Micrometer.</p>

      <p><strong>Gradle Kotlin DSL</strong></p>

      <pre class="mr-code-block"><code>implementation("io.microraft:microraft-metrics:0.9")</code></pre>

      <p><strong>Maven</strong></p>

      <pre class="mr-code-block"><code>&lt;dependency&gt;
    &lt;groupId&gt;io.microraft&lt;/groupId&gt;
    &lt;artifactId&gt;microraft-metrics&lt;/artifactId&gt;
    &lt;version&gt;0.9&lt;/version&gt;
&lt;/dependency&gt;</code></pre>
    </article>

    <article class="mr-doc-card">
      <h3>What to alert on</h3>
      <ul>
        <li>unexpected leader changes</li>
        <li>quorum loss or sustained follower isolation</li>
        <li>replication lag and commit index drift</li>
        <li>snapshot churn and recovery loops</li>
      </ul>
      <p>Continue with the <a href="/docs/production-checklist/">Production Checklist</a> for rollout-level guidance.</p>
      <a class="mr-card-cta" href="/docs/production-checklist/">Open checklist</a>
    </article>
  </section>
</div>
