---
seo_title: "MicroRaft Interactive Demo for Java Raft Leader Election and Replication"
description: "Use the MicroRaft interactive demo to understand Java Raft leader election, quorum, log replication, partitions, and recovery behavior."
keywords: "raft demo java, microraft demo, leader election demo, log replication demo, quorum demo"
schema_type: SoftwareApplication
og_type: website
---
<div class="mr-doc-shell mr-demo-intro">
  <section class="mr-doc-hero mr-doc-hero-compact">
    <div class="mr-page-kicker">Interactive walkthrough</div>
    <h1 class="mr-page-title">Interactive Demo</h1>
    <p class="mr-page-summary">
      This page gives you a lightweight feel for how a small MicroRaft cluster
      behaves. It is not a protocol-accurate simulator; it is a teaching surface that
      shows the core ideas quickly: leader election, replication, partition, and
      recovery.
    </p>
  </section>
  <section class="mr-doc-grid mr-demo-guide">
    <article class="mr-doc-card">
      <h3>How to use this demo</h3>
      <p>Start with <strong>Happy path</strong>, then try <strong>Follower partition</strong>, then <strong>Recovery</strong>. That sequence explains most of what matters.</p>
      <ol>
        <li>Pick a client command.</li>
        <li>Watch which node is leader and whether quorum is still healthy.</li>
        <li>Use the explanation panel to understand what just changed.</li>
      </ol>
    </article>
    <article class="mr-doc-card">
      <h3>What the key terms mean</h3>
      <ul>
        <li><strong>Leader</strong> accepts writes first.</li>
        <li><strong>Quorum</strong> means a majority of Raft nodes can still agree.</li>
        <li><strong>Commit index</strong> is the highest entry safely committed by the cluster.</li>
      </ul>
    </article>
  </section>
</div>

<div id="mr-demo" class="mr-demo-shell">
  <div class="mr-demo-toolbar">
    <div class="mr-demo-control-group mr-demo-control-group-command">
      <span class="mr-demo-control-label">Client command</span>
      <div class="mr-demo-control-row">
        <select id="mr-demo-command" class="mr-demo-select" aria-label="Select demo command">
          <option value="set value=42">set value=42</option>
          <option value="compare-and-set value=42->84">compare-and-set value=42->84</option>
          <option value="get value">get value</option>
        </select>
        <button id="mr-demo-replicate" class="mr-button mr-button-primary" type="button">Replicate command</button>
      </div>
    </div>
    <div class="mr-demo-control-group mr-demo-control-group-cluster">
      <span class="mr-demo-control-label">Cluster controls</span>
      <div class="mr-demo-control-actions">
        <button id="mr-demo-elect" class="mr-button mr-button-ghost" type="button">Rotate leader</button>
        <button id="mr-demo-partition" class="mr-button mr-button-ghost" type="button">Partition node-c</button>
        <button id="mr-demo-recover" class="mr-button mr-button-ghost" type="button">Recover node-c</button>
        <button id="mr-demo-reset" class="mr-button mr-button-secondary" type="button">Reset demo</button>
      </div>
    </div>
  </div>
  <div class="mr-demo-layout">
    <div class="mr-demo-canvas">
      <div id="mr-cluster" class="mr-cluster">
        <svg id="mr-demo-links" viewBox="0 0 760 360" preserveAspectRatio="none"></svg>
        <div id="mr-demo-particles" class="mr-particle-layer" aria-hidden="true"></div>
      </div>
      <div class="mr-demo-message-strip">
        <div class="mr-message-card">
          <strong>Client command</strong>
          <span id="mr-demo-command-view">set value=42</span>
        </div>
        <div class="mr-message-card">
          <strong>Quorum status</strong>
          <span id="mr-demo-quorum-badge" class="mr-quorum-badge is-healthy">Healthy quorum</span>
          <span id="mr-demo-quorum">3/3 raft nodes available</span>
        </div>
        <div class="mr-message-card">
          <strong>Commit result</strong>
          <span id="mr-demo-outcome">Ready to replicate</span>
        </div>
      </div>
      <div class="mr-demo-facts">
        <div class="mr-fact">
          <strong id="mr-demo-term">1</strong>
          <span>current term</span>
        </div>
        <div class="mr-fact">
          <strong id="mr-demo-leader">node-b</strong>
          <span>current leader</span>
        </div>
        <div class="mr-fact">
          <strong id="mr-demo-commit">1</strong>
          <span>commit index</span>
        </div>
      </div>
      <div class="mr-demo-legend">
        <div class="mr-demo-legend-item">
          <span class="mr-demo-legend-swatch is-leader"></span>
          <span>Leader accepts writes and coordinates replication.</span>
        </div>
        <div class="mr-demo-legend-item">
          <span class="mr-demo-legend-swatch is-follower"></span>
          <span>Followers acknowledge entries and apply committed state in order.</span>
        </div>
        <div class="mr-demo-legend-item">
          <span class="mr-demo-legend-swatch is-cut"></span>
          <span>Dashed links and faded nodes mean a follower is partitioned.</span>
        </div>
      </div>
    </div>
    <div class="mr-demo-panel">
      <div class="mr-demo-panel-card mr-demo-panel-mode">
        <div class="mr-demo-panel-mode-head">
          <div>
            <p class="mr-demo-panel-kicker">Panel mode</p>
            <h3 class="mr-demo-panel-title">Choose your level</h3>
          </div>
          <div class="mr-demo-mode-switch" role="tablist" aria-label="Demo detail level">
            <button id="mr-demo-mode-beginner" class="mr-demo-mode-button is-active" type="button" role="tab" aria-selected="true" data-mr-demo-mode="beginner">
              Beginner
            </button>
            <button id="mr-demo-mode-deep" class="mr-demo-mode-button" type="button" role="tab" aria-selected="false" data-mr-demo-mode="deep">
              Deep dive
            </button>
          </div>
        </div>
        <p id="mr-demo-mode-summary" class="mr-demo-mode-summary">
          Beginner mode keeps the right side focused on explanation, tour, and presets. Switch to Deep dive for the raw trace.
        </p>
      </div>
      <div class="mr-demo-panel-card mr-demo-panel-card-emphasis">
        <p class="mr-demo-panel-kicker">Live explanation</p>
        <h3 class="mr-demo-panel-title">What you are seeing</h3>
        <div class="mr-demo-explainer">
          <p id="mr-demo-explainer-title" class="mr-demo-explainer-title">Cluster is healthy and ready for a new client command.</p>
          <p id="mr-demo-explainer-body" class="mr-demo-explainer-body">A healthy leader can accept writes, replicate them to followers, and commit once quorum acknowledges the entry.</p>
        </div>
      </div>
      <div class="mr-demo-panel-card mr-demo-tour" data-mr-demo-view="beginner">
        <div class="mr-demo-tour-head">
          <div>
            <p class="mr-demo-tour-kicker">Guided tour</p>
            <h3 id="mr-demo-tour-title" class="mr-demo-tour-title">Walk the cluster step by step</h3>
          </div>
          <span id="mr-demo-tour-step" class="mr-demo-tour-step">Not started</span>
        </div>
        <p id="mr-demo-tour-body" class="mr-demo-tour-body">Start the guided tour to see a healthy write, follower loss, continued quorum, and recovery in the shortest possible order.</p>
        <p id="mr-demo-tour-next-hint" class="mr-demo-tour-next-hint" hidden>Coming next: the leader will replicate a healthy command.</p>
        <button id="mr-demo-happy-path" class="mr-button mr-button-ghost mr-demo-happy-button" type="button">Show me the happy path</button>
        <label class="mr-demo-tour-speed" for="mr-demo-tour-speed">
          <span>Autoplay speed</span>
          <select id="mr-demo-tour-speed" class="mr-demo-tour-speed-select">
            <option value="2400">Fast</option>
            <option value="3200" selected>Normal</option>
            <option value="4500">Slow</option>
          </select>
        </label>
        <div class="mr-demo-tour-actions">
          <button id="mr-demo-tour-start" class="mr-button mr-button-primary" type="button">Start tour</button>
          <button id="mr-demo-tour-auto" class="mr-button mr-button-ghost" type="button">Autoplay tour</button>
          <button id="mr-demo-tour-next" class="mr-button mr-button-ghost" type="button" disabled>Next step</button>
          <button id="mr-demo-tour-stop" class="mr-button mr-button-secondary" type="button" disabled>Exit tour</button>
        </div>
      </div>
      <div class="mr-demo-side-grid" data-mr-demo-view="deep" hidden>
        <section class="mr-demo-panel-card">
          <p class="mr-demo-panel-kicker">Trace</p>
          <h3 class="mr-demo-panel-title">Timeline</h3>
          <ol id="mr-demo-timeline" class="mr-timeline"></ol>
        </section>
        <section class="mr-demo-panel-card">
          <p class="mr-demo-panel-kicker">Protocol view</p>
          <h3 class="mr-demo-panel-title">Protocol flow</h3>
          <ol id="mr-demo-flow" class="mr-flow-list"></ol>
        </section>
      </div>
      <section class="mr-demo-panel-card" data-mr-demo-view="beginner">
        <p class="mr-demo-panel-kicker">Fast presets</p>
        <h3 class="mr-demo-panel-title">Preset scenarios</h3>
        <div class="mr-scenario-list">
          <button id="mr-demo-scenario-happy" class="mr-scenario" type="button">
            <strong>Happy path</strong>
            <span>Leader replicates a new command to quorum.</span>
          </button>
          <button id="mr-demo-scenario-partition" class="mr-scenario" type="button">
            <strong>Follower partition</strong>
            <span>One follower drops, quorum still commits.</span>
          </button>
          <button id="mr-demo-scenario-recovery" class="mr-scenario" type="button">
            <strong>Recovery</strong>
            <span>Isolated node reconnects and catches up.</span>
          </button>
        </div>
      </section>
      <section class="mr-demo-panel-card" data-mr-demo-view="deep" hidden>
        <p class="mr-demo-panel-kicker">Interactive presets</p>
        <h3 class="mr-demo-panel-title">Preset scenarios</h3>
        <div class="mr-scenario-list">
          <button id="mr-demo-scenario-happy-deep" class="mr-scenario" type="button">
            <strong>Happy path</strong>
            <span>Leader replicates a new command to quorum.</span>
          </button>
          <button id="mr-demo-scenario-partition-deep" class="mr-scenario" type="button">
            <strong>Follower partition</strong>
            <span>One follower drops, quorum still commits.</span>
          </button>
          <button id="mr-demo-scenario-recovery-deep" class="mr-scenario" type="button">
            <strong>Recovery</strong>
            <span>Isolated node reconnects and catches up.</span>
          </button>
        </div>
      </section>
      <section class="mr-demo-panel-card" data-mr-demo-view="deep" hidden>
        <p class="mr-demo-panel-kicker">Raw events</p>
        <h3 class="mr-demo-panel-title">Event log</h3>
        <p class="mr-muted">Use the controls to watch the cluster state change.</p>
        <ul id="mr-demo-log" class="mr-log"></ul>
      </section>
    </div>
  </div>
</div>

## Map the demo to the real code

<div class="mr-doc-grid mr-resource-grid">
  <a class="mr-doc-card mr-resource-card" href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/LeaderElectionTest.java">
    <strong class="mr-resource-card-title">Leader election</strong>
    <span class="mr-resource-card-summary">The leader rotation idea in this demo maps to the runnable tutorial test below.</span>
    <span class="mr-card-cta">View source</span>
  </a>
  <a class="mr-doc-card mr-resource-card" href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/OperationCommitTest.java">
    <strong class="mr-resource-card-title">Log replication</strong>
    <span class="mr-resource-card-summary">The "Replicate Command" flow corresponds to the operation commit scenario.</span>
    <span class="mr-card-cta">View source</span>
  </a>
  <a class="mr-doc-card mr-resource-card" href="https://github.com/MicroRaft/MicroRaft/tree/master/microraft-tutorial/src/main/java/io/microraft/tutorial/atomicregister">
    <strong class="mr-resource-card-title">State machine</strong>
    <span class="mr-resource-card-summary">The demo commands represent the small atomic register state machine used in the tutorial module.</span>
    <span class="mr-card-cta">View source</span>
  </a>
  <a class="mr-doc-card mr-resource-card" href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/BaseLocalTest.java">
    <strong class="mr-resource-card-title">Local cluster wiring</strong>
    <span class="mr-resource-card-summary">The in-browser nodes are a teaching abstraction; the local transport and test harness show the real in-process setup.</span>
    <span class="mr-card-cta">View source</span>
  </a>
</div>

## What this demo is good for

- showing new users what leader election feels like,
- making replication and quorum language less abstract,
- giving the website a runnable surface instead of only static text.

## What the states mean

- A leader accepts writes and coordinates replication.
- Followers apply committed entries in the same order.
- A single isolated follower does not stop progress because quorum still exists.
- When the follower comes back, it catches up to the latest commit index.
- The preset scenarios are the shortest way to explain quorum behavior in talks, onboarding, or design docs.

## What to do next

- Want the real local walkthrough? Go to [Setup](docs/setup.md).
- Want to build your own state machine? Read [Tutorial: Building an Atomic Register](docs/tutorial-building-an-atomic-register.md).
- Want the positioning story first? Read [Why MicroRaft?](docs/why-microraft.md).
