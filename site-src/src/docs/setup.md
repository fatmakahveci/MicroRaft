---
tab_title: "Setup"
seo_title: "MicroRaft Setup Guide for Java Raft Projects and Local Tutorial Runs"
description: "Set up MicroRaft in a Java project, add the dependencies, and run the local tutorial flow with Gradle."
keywords: "MicroRaft setup, Java Raft setup, Gradle Raft tutorial, MicroRaft dependencies, Java consensus setup"
schema_type: TechArticle
og_type: article
doc_layout: reference
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Setup</h1>
    <p class="mr-page-summary">Get MicroRaft into your project or run the repository locally in a few minutes.</p>
    <div class="mr-inline-code"><code>./gradlew :microraft-tutorial:test --tests io.microraft.tutorial.OperationCommitTest</code></div>
  </section>
  <section class="mr-doc-summary">
    <article class="mr-doc-summary-card">
      <strong>Use this page when</strong>
      <span>You want the fastest path from clone or dependency declaration to a visible local run.</span>
    </article>
    <article class="mr-doc-summary-card">
      <strong>Leave with</strong>
      <span>A working local command, dependency coordinates, and the right next docs to open.</span>
    </article>
    <article class="mr-doc-summary-card">
      <strong>Read next</strong>
      <span>Go to Main Abstractions if you are integrating, or the tutorial if you want runnable code first.</span>
    </article>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Add the dependency</h3>
      <p>MicroRaft JARs are available via Maven Central.</p>

      <p><strong>Gradle version catalog</strong></p>

      <pre class="mr-code-block"><code>[versions]
microraft = "0.9"

[libraries]
microraft = { module = "io.microraft:microraft", version.ref = "microraft" }</code></pre>

      <p><strong>Gradle Kotlin DSL</strong></p>

      <pre class="mr-code-block"><code>implementation("io.microraft:microraft:0.9")</code></pre>

      <p><strong>Maven</strong></p>

      <pre class="mr-code-block"><code>&lt;dependency&gt;
    &lt;groupId&gt;io.microraft&lt;/groupId&gt;
    &lt;artifactId&gt;microraft&lt;/artifactId&gt;
    &lt;version&gt;0.9&lt;/version&gt;
&lt;/dependency&gt;</code></pre>
    </article>

    <article class="mr-doc-card">
      <h3>Build from source</h3>
      <p>If you want to inspect or run the repository locally, clone it and use the Gradle wrapper.</p>

      <pre class="mr-code-block"><code>gh repo clone MicroRaft/MicroRaft
cd MicroRaft
./gradlew build</code></pre>

      <p>The built artifacts land under <code>microraft/build/libs</code>, <code>microraft-hocon/build/libs</code>, and <code>microraft-yaml/build/libs</code>.</p>
    </article>

    <article class="mr-doc-card">
      <h3>Quick Start</h3>
      <p>Run the local 3-node tutorial cluster before integrating the library into your own service.</p>

      <pre class="mr-code-block"><code>./gradlew :microraft-tutorial:test \
  --tests io.microraft.tutorial.OperationCommitTest</code></pre>

      <ul>
        <li>the build targets Java 11</li>
        <li>make sure Java 11 is installed locally before running the command</li>
        <li><code>LeaderElectionTest</code> is the shortest smoke test for cluster formation</li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>Logging and next steps</h3>
      <p>MicroRaft depends only on SLF4J for logging. Enable <code>INFO</code> for <code>io.microraft</code>; use <code>DEBUG</code> only when you actually want deep protocol noise.</p>
      <ul>
        <li><a href="/docs/why-microraft/">Why MicroRaft?</a></li>
        <li><a href="/docs/main-abstractions/">Main Abstractions</a></li>
        <li><a href="/docs/tutorial-building-an-atomic-register/">Tutorial: Building an Atomic Register</a></li>
        <li><a href="/docs/troubleshooting/">Troubleshooting</a></li>
      </ul>
      <a class="mr-card-cta" href="/docs/main-abstractions/">Open abstractions</a>
    </article>
  </section>
</div>
