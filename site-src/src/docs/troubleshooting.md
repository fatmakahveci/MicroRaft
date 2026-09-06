---
tab_title: "Troubleshooting"
seo_title: "MicroRaft Troubleshooting for Java Raft Leader Changes, Quorum, and Startup Issues"
description: "Troubleshoot MicroRaft startup problems, repeated leader changes, quorum loss, stalled recovery, and misconfiguration in Java Raft systems."
keywords: "MicroRaft troubleshooting, Java Raft troubleshooting, repeated leader changes, quorum troubleshooting, startup issues, stalled recovery"
schema_type: TechArticle
og_type: article
doc_layout: reference
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Troubleshooting</h1>
    <p class="mr-page-summary">
      This page covers the most common first-run issues for MicroRaft users.
      The format is symptom, cause, and fix so you can recover quickly.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Gradle cannot find Java 11</h3>

      <p><strong>Symptom</strong>: the build fails before running tests and Gradle reports that it cannot find a Java installation matching version 11.</p>

      <p><strong>Why it happens</strong>: the repository build targets Java 11.</p>

      <p><strong>What to do</strong>:</p>

      <ul>
        <li>install Java 11 locally</li>
        <li>rerun the original Gradle command after Java 11 is available</li>
      </ul>

      <pre class="mr-code-block"><code>./gradlew :microraft-tutorial:test \
  --tests io.microraft.tutorial.OperationCommitTest</code></pre>
    </article>

    <article class="mr-doc-card">
      <h3>I used the wrong build command</h3>

      <p><strong>Symptom</strong>: you copied an older Maven command from a stale source and <code>mvnw</code> does not reflect the current repository build flow.</p>

      <p><strong>What to do</strong>:</p>

      <ul>
        <li>use <code>./gradlew</code>, not <code>mvnw</code></li>
        <li>prefer the commands in the root README or <a href="/docs/setup/">Setup</a></li>
      </ul>
    </article>

    <article class="mr-doc-card">
      <h3>No leader seems to be elected locally</h3>

      <p><strong>Symptom</strong>: the scenario hangs or never reaches the expected leader-dependent assertions.</p>

      <p><strong>What to do</strong>:</p>

      <ul>
        <li>start with <code>LeaderElectionTest</code> to isolate cluster formation</li>
        <li>confirm you are running the tutorial test module from the repository root</li>
        <li>check your logs for local transport setup failures before assuming a protocol issue</li>
      </ul>

      <pre class="mr-code-block"><code>./gradlew :microraft-tutorial:test \
  --tests io.microraft.tutorial.LeaderElectionTest</code></pre>
    </article>

    <article class="mr-doc-card">
      <h3>The example command is not found</h3>

      <p><strong>Symptom</strong>: your shell cannot find <code>./gradlew</code>, or the module path or test class name is not recognized.</p>

      <p><strong>What to do</strong>:</p>

      <ul>
        <li>run the command from the repository root</li>
        <li>make sure the repository clone completed successfully</li>
        <li>use the fully qualified test names shown in <a href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/README.md" target="_blank" rel="noopener noreferrer">microraft-tutorial/README.md</a></li>
      </ul>
    </article>
  </section>

  <section class="mr-doc-band">
    <h2>Still blocked?</h2>
    <ul>
      <li>open a GitHub issue with Java version, OS, logs, and exact command line</li>
      <li>start a discussion if you are unsure whether the behavior is a bug or a usage issue</li>
    </ul>
  </section>
</div>
