---
seo_title: "MicroRaft FAQ for Java Raft, Consensus, Snapshots, and Production Use"
description: "Read the MicroRaft FAQ covering what MicroRaft is, when to use it, whether it is a database, and how snapshots, monitoring, and production rollout work."
keywords: "microraft faq, java raft faq, raft library java questions, microraft snapshots, microraft monitoring"
schema_type: FAQPage
og_type: website
doc_layout: reference
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">FAQ</h1>
    <p class="mr-page-summary">
      These are the questions people usually ask before they decide whether
      MicroRaft belongs inside a Java service.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>What is MicroRaft?</h3>
      <p>MicroRaft is an embeddable Java Raft library. It gives you a consensus core, while you still own transport, persistence, and state machine logic.</p>
    </article>

    <article class="mr-doc-card">
      <h3>Is MicroRaft a database?</h3>
      <p>No. It is a library for building CP subsystems such as metadata stores, coordination services, and lock services.</p>
    </article>

    <article class="mr-doc-card">
      <h3>When should I use it?</h3>
      <p>Use it when you need strong consistency inside a Java system you already own and you want control over integration boundaries.</p>
    </article>

    <article class="mr-doc-card">
      <h3>What does it already support?</h3>
      <p>Snapshotting, membership changes, quorum-aware queries, leadership transfer, and monitoring surfaces are already part of the library story.</p>
    </article>
  </section>

  <section class="mr-doc-band">
    <h2>More answers</h2>
    <ol>
      <li><a href="/docs/why-microraft/">Why MicroRaft?</a></li>
      <li><a href="/docs/main-abstractions/">Main Abstractions</a></li>
      <li><a href="/docs/monitoring/">Monitoring</a></li>
      <li><a href="/docs/production-checklist/">Production Checklist</a></li>
    </ol>
    <span class="mr-card-cta">Read guide</span>
  </section>

  <script type="application/ld+json">
    {
      "@context": "https://schema.org",
      "@type": "FAQPage",
      "mainEntity": [
        {
          "@type": "Question",
          "name": "What is MicroRaft?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "MicroRaft is an embeddable Java Raft library. It gives you a consensus core while you still own transport, persistence, and state machine logic."
          }
        },
        {
          "@type": "Question",
          "name": "Is MicroRaft a database?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "No. It is a library for building CP subsystems such as metadata stores, coordination services, and lock services."
          }
        },
        {
          "@type": "Question",
          "name": "When should I use it?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "Use MicroRaft when you need strong consistency inside a Java system you already own and you want control over integration boundaries."
          }
        },
        {
          "@type": "Question",
          "name": "What does it already support?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "Snapshotting, membership changes, quorum-aware queries, leadership transfer, and monitoring surfaces are already part of the library story."
          }
        }
      ]
    }
  </script>
</div>
