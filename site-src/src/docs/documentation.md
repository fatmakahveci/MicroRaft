---
tab_title: "Documentation"
seo_title: "MicroRaft Documentation for Java Raft Setup, Integration, and Operations"
description: "Start the MicroRaft docs with setup, evaluation, integration, troubleshooting, and production guidance for Java Raft systems."
keywords: "MicroRaft documentation, Java Raft docs, MicroRaft setup guide, MicroRaft integration, MicroRaft operations"
schema_type: CollectionPage
og_type: website
doc_layout: reference
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Documentation hub</h1>
    <p class="mr-page-summary">
      Do not read the whole site linearly. Pick one path: understand the project,
      wire it into a service, or harden a real deployment.
    </p>
  </section>
  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>New here</h3>
      <p>Start with the smallest path that shows what MicroRaft is and how it behaves.</p>
      <ol>
        <li><a href="/docs/setup/">Setup</a></li>
        <li><a href="/docs/tutorial-building-an-atomic-register/">Atomic Register Tutorial</a></li>
        <li><a href="/docs/main-abstractions/">Main Abstractions</a></li>
      </ol>
      <a class="mr-card-cta" href="/docs/setup/">Open path</a>
    </article>

    <article class="mr-doc-card">
      <h3>Evaluate fit</h3>
      <p>Use this path if you want to know whether an embeddable Java Raft core is the right choice.</p>
      <ol>
        <li><a href="/docs/why-microraft/">Why MicroRaft?</a></li>
        <li><a href="/docs/use-cases/">Use Cases</a></li>
        <li><a href="/docs/recipe-metadata-store/">Metadata Store Recipe</a></li>
      </ol>
      <a class="mr-card-cta" href="/docs/why-microraft/">Open path</a>
    </article>

    <article class="mr-doc-card">
      <h3>Build and operate</h3>
      <p>Use this once you are ready to integrate the library and prepare it for production.</p>
      <ol>
        <li><a href="/docs/main-abstractions/">Main Abstractions</a></li>
        <li><a href="/docs/configuration/">Configuration</a></li>
        <li><a href="/docs/production-checklist/">Production Checklist</a></li>
      </ol>
      <a class="mr-card-cta" href="/docs/main-abstractions/">Open path</a>
    </article>
  </section>

  <section class="mr-doc-band">
    <h2>Useful supporting reads</h2>
    <div class="mr-card-grid">
      <a class="mr-card mr-home-link-card" href="/docs/troubleshooting/">
        <h3>Troubleshooting</h3>
        <p>Jump here when rollout, networking, or state-machine behavior does not look right.</p>
        <span class="mr-card-cta">Open troubleshooting</span>
      </a>
      <a class="mr-card mr-home-link-card" href="/docs/resiliency-and-fault-tolerance/">
        <h3>Failure behavior</h3>
        <p>Read resiliency and fault-tolerance notes before you reason about outages and degraded quorum.</p>
        <span class="mr-card-cta">Open resiliency guide</span>
      </a>
      <a class="mr-card mr-home-link-card" href="/docs/monitoring/">
        <h3>Monitoring</h3>
        <p>Use reports and metrics once you need term, leader, quorum, and lag signals in production.</p>
        <span class="mr-card-cta">Open monitoring</span>
      </a>
      <a class="mr-card mr-home-link-card" href="https://github.com/MicroRaft/MicroRaft" target="_blank" rel="noopener noreferrer">
        <h3>Source and API reference</h3>
        <p>Go to GitHub and Javadoc once you are integrating concrete RaftNode, transport, and persistence code.</p>
        <span class="mr-card-cta mr-card-cta-github"><span class="fa-brands fa-github" aria-hidden="true"></span><span>Open source and API</span></span>
      </a>
    </div>
  </section>
</div>
