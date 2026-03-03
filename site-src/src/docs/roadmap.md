---
seo_title: "MicroRaft Roadmap and Direction for Java Raft Development"
description: "Understand the MicroRaft roadmap and direction for future Java Raft capabilities, without treating it as a fixed release promise."
keywords: "microraft roadmap, java raft roadmap, microraft future, raft library roadmap java"
schema_type: CollectionPage
og_type: website
doc_layout: reference
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Roadmap</h1>
    <p class="mr-page-summary">
      These are areas of interest, not promised delivery dates. Use them to
      understand direction, not to plan against a fixed release schedule.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Protocol and correctness work</h3>
      <ul class="mr-doc-list">
        <li>opt-in deduplication mechanisms</li>
        <li>witness replicas and quorum-shaping ideas</li>
      </ul>
    </article>
    <article class="mr-doc-card">
      <h3>Replication improvements</h3>
      <ul class="mr-doc-list">
        <li>offloading more work from leader to followers</li>
        <li>smarter log catch-up behavior after snapshots</li>
        <li>more adaptive append entries batching and retry behavior</li>
      </ul>
    </article>
    <article class="mr-doc-card">
      <h3>Feedback</h3>
      <p>
        If you have ideas or want to discuss tradeoffs, join the
        <a href="https://join.slack.com/t/microraft/shared_invite/zt-dc6utpfk-84P0VbK7EcrD3lIme2IaaQ" target="_blank" rel="noreferrer">community Slack</a>.
      </p>
      <span class="mr-card-cta">Join discussion</span>
    </article>
  </section>
</div>

- Opt-in deduplication mechanism via implementation of the [Implementing
  Linearizability at Large Scale and Low
  Latency](https://dl.acm.org/doi/10.1145/2815400.2815416) paper. Currently, one
  can implement deduplication inside his custom `StateMachine` implementation. I
  would like to offer a generic and opt-in solution by MicroRaft.

- Witness replicas possibly via implementation of the [Pirogue, a lighter
  dynamic version of the Raft distributed consensus
  algorithm](https://dl.acm.org/doi/10.1109/PCCC.2015.7410281) paper. Witness
  replicas participate in quorum calculations but do not keep any state for
  `StateMachine` to reduce the storage overhead. When a follower fails, a
  witness replica can be promoted to the follower role to increase the number of
  `StateMachine` replicas.

- Offload more work from leader to followers. One candidate is transfer of
  committed log entries. Just like parallel snapshot chunk transfer from
  followers, a slow follower can get committed log entries from followers.

- Improve the log replication design. The current log replication design is
  quite solid but there is still room for improvement. One idea is, once a
  follower installs a snapshot, the leader can boost that follower by increasing
  its *Append Entries RPC* batch size, so that it catches up with the majority
  faster. Another thing to try is, currently when a leader sends an *Append
  Entries RPC* to a follower, it does not send another RPC to that follower
  either until the follower sends a response, or the *Append Entries RPC
  backoff* timeout elapses. During this duration, the leader might append new
  log entries in its local log. During the *Append Entries RPC backoff* is
  enabled for a follower, if more log entries are appended to the leader's log,
  a few of these log entries can be also sent to the follower.
