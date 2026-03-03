---
seo_title: "MicroRaft Interactive Demo for Java Raft Leader Election and Replication"
description: "Use the MicroRaft interactive demo to step through Java Raft quorum, leader failover, learner promotion, snapshots, and recovery behavior."
keywords: "MicroRaft demo, Java Raft demo, quorum demo, leader failover demo, learner promotion demo, snapshot catch up demo, log replication demo"
schema_type: SoftwareApplication
og_type: website
---
<div class="mr-doc-shell mr-demo-intro">
  <section class="mr-doc-hero mr-doc-hero-compact">
    <div class="mr-page-kicker">Interactive walkthrough</div>
    <h1 class="mr-page-title">Interactive Demo</h1>
    <p class="mr-page-summary">
      Start with healthy replication, then move through loss, recovery, failover, and membership changes.
    </p>
  </section>
  <section class="mr-doc-band">
    <p class="mr-demo-intro-note">
      Start with <strong>Healthy write</strong>, then <strong>Follower loss</strong>, then <strong>Recovery</strong>. Open <strong>Setup</strong> if you want the runnable code path next.
    </p>
  </section>
</div>

<div id="mr-demo" class="mr-demo-shell">
  <a class="mr-demo-skip-link" href="#mr-cluster-stage">Skip to cluster diagram</a>
  <div id="mr-demo-onboarding" class="mr-demo-onboarding" hidden>
    <div class="mr-demo-onboarding-backdrop" data-mr-demo-onboarding-close></div>
    <div class="mr-demo-onboarding-dialog" role="dialog" aria-modal="true" aria-labelledby="mr-demo-onboarding-title">
      <p class="mr-demo-onboarding-kicker">Start here</p>
      <h2 id="mr-demo-onboarding-title" class="mr-demo-onboarding-title">How to use this demo</h2>
      <p class="mr-demo-onboarding-body">
        Follow the preset states in order. Start with a healthy write, then move through loss, recovery, failover, and membership changes.
      </p>
      <ol class="mr-demo-onboarding-steps">
        <li>Watch the graph first.</li>
        <li>Use the walkthrough for the recommended order.</li>
        <li>Use a preset to jump to one known state.</li>
      </ol>
      <div class="mr-demo-onboarding-actions">
        <button id="mr-demo-onboarding-start" class="mr-button mr-button-primary" type="button">Start walkthrough</button>
        <button id="mr-demo-onboarding-explore" class="mr-button mr-button-ghost" type="button">I will explore manually</button>
      </div>
      <label class="mr-demo-onboarding-persist">
        <input id="mr-demo-onboarding-persist" type="checkbox" />
        <span>Don't show again</span>
      </label>
    </div>
  </div>
  <div class="mr-demo-surface">
    <section class="mr-demo-panel-card mr-demo-panel-card-flat mr-demo-usage-panel" aria-labelledby="mr-demo-usage-title">
      <p class="mr-demo-panel-kicker">Use this page in 3 steps</p>
      <h2 id="mr-demo-usage-title" class="mr-demo-panel-title">Pick a preset, watch the cluster, then read the explanation</h2>
      <ol class="mr-demo-usage-list">
        <li>Start with <strong>Healthy write</strong> or <strong>Start walkthrough</strong>.</li>
        <li>Watch the node graph first.</li>
        <li>Then read <strong>Current state</strong> or switch to <strong>Deep dive</strong>.</li>
      </ol>
    </section>
    <section class="mr-demo-panel-card mr-demo-panel-card-flat mr-demo-preset-panel" data-mr-demo-view="beginner" aria-labelledby="mr-demo-preset-title" aria-describedby="mr-demo-preset-help">
      <div class="mr-demo-tour-head mr-demo-preset-head">
        <div>
          <p class="mr-demo-panel-kicker">Preset walkthrough</p>
          <h2 id="mr-demo-preset-title" class="mr-demo-panel-title">Jump to a known cluster state</h2>
        </div>
        <div class="mr-demo-share-row">
          <button class="mr-button mr-button-ghost mr-demo-share-button" type="button" data-mr-demo-share>
            <span class="fa-solid fa-link" aria-hidden="true"></span>
            <span class="mr-demo-share-label">Copy state link</span>
          </button>
        </div>
      </div>
      <p id="mr-demo-preset-help" class="mr-demo-preset-help">Choose a preset to jump to a known state.</p>
      <p id="mr-demo-scenario-status" class="mr-demo-scenario-status" aria-live="polite">No preset selected yet. Start with the healthy write.</p>
      <div class="mr-demo-quick-paths">
        <button id="mr-demo-goal-healthy" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">1</span>
          <strong>Healthy write</strong>
          <span>Normal leader append and quorum commit.</span>
        </button>
        <button id="mr-demo-goal-quorum" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">2</span>
          <strong>Quorum under follower loss</strong>
          <span>Lose one follower and commit with the remaining majority.</span>
        </button>
        <button id="mr-demo-goal-recovery" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">3</span>
          <strong>Follower recovery</strong>
          <span>Bring the isolated follower back and catch it up.</span>
        </button>
        <button id="mr-demo-goal-snapshot" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">4</span>
          <strong>Snapshot catch-up</strong>
          <span>Recover a far-behind follower with a snapshot.</span>
        </button>
        <button id="mr-demo-goal-failover" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">5</span>
          <strong>Leader failover</strong>
          <span>Lose the leader and elect a new one.</span>
        </button>
        <button id="mr-demo-goal-backpressure" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">6</span>
          <strong>Backpressure under load</strong>
          <span>See the leader reject new work under load.</span>
        </button>
        <button id="mr-demo-goal-learner" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">7</span>
          <strong>Add learner</strong>
          <span>Add a non-voting learner without changing quorum.</span>
        </button>
        <button id="mr-demo-goal-promote-learner" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">8</span>
          <strong>Promote learner</strong>
          <span>Promote the learner and grow majority to 3-of-4.</span>
        </button>
        <button id="mr-demo-goal-four-node-quorum" class="mr-scenario mr-scenario-inline" type="button" aria-pressed="false">
          <span class="mr-scenario-order">9</span>
          <strong>3-of-4 quorum</strong>
          <span>After promotion, lose one follower and keep committing with 3-of-4.</span>
        </button>
      </div>
    </section>
    <div class="mr-demo-layout">
      <div class="mr-demo-canvas-card">
        <div class="mr-demo-canvas">
          <section class="mr-demo-guide-block" data-mr-demo-view="beginner" aria-labelledby="mr-demo-tour-title">
            <div class="mr-demo-panel-card mr-demo-tour mr-demo-panel-card-flat">
              <div class="mr-demo-tour-head">
                <div>
                  <p class="mr-demo-tour-kicker">Guided walkthrough</p>
                  <h3 id="mr-demo-tour-title" class="mr-demo-tour-title">Follow the demo in the right order</h3>
                </div>
                <span id="mr-demo-tour-step" class="mr-demo-tour-step">Not started</span>
              </div>
              <div id="mr-demo-stepper" class="mr-demo-stepper" aria-label="Demo walkthrough steps">
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="0" aria-current="false">
                  <span class="mr-demo-stepper-index">1</span>
                  <span class="mr-demo-stepper-label">Healthy write</span>
                </button>
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="1" aria-current="false">
                  <span class="mr-demo-stepper-index">2</span>
                  <span class="mr-demo-stepper-label">Follower loss</span>
                </button>
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="2" aria-current="false">
                  <span class="mr-demo-stepper-index">3</span>
                  <span class="mr-demo-stepper-label">Recovery</span>
                </button>
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="3" aria-current="false">
                  <span class="mr-demo-stepper-index">4</span>
                  <span class="mr-demo-stepper-label">Snapshot catch-up</span>
                </button>
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="4" aria-current="false">
                  <span class="mr-demo-stepper-index">5</span>
                  <span class="mr-demo-stepper-label">Leader failover</span>
                </button>
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="5" aria-current="false">
                  <span class="mr-demo-stepper-index">6</span>
                  <span class="mr-demo-stepper-label">Backpressure</span>
                </button>
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="6" aria-current="false">
                  <span class="mr-demo-stepper-index">7</span>
                  <span class="mr-demo-stepper-label">Add learner</span>
                </button>
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="7" aria-current="false">
                  <span class="mr-demo-stepper-index">8</span>
                  <span class="mr-demo-stepper-label">Promote learner</span>
                </button>
                <button class="mr-demo-stepper-item" type="button" data-mr-tour-jump="8" aria-current="false">
                  <span class="mr-demo-stepper-index">9</span>
                  <span class="mr-demo-stepper-label">3-of-4 quorum</span>
                </button>
              </div>
              <p id="mr-demo-tour-body" class="mr-demo-tour-body">Step through the preset states in docs order.</p>
              <p id="mr-demo-tour-next-hint" class="mr-demo-tour-next-hint" hidden>Coming next: the leader will replicate a healthy command.</p>
              <div class="mr-demo-tour-toolbar">
                <button id="mr-demo-happy-path" class="mr-button mr-button-ghost mr-demo-happy-button" type="button">Jump to healthy write</button>
                <label class="mr-demo-tour-speed" for="mr-demo-tour-speed">
                  <span>Autoplay speed</span>
                  <select id="mr-demo-tour-speed" class="mr-demo-tour-speed-select">
                    <option value="2400">Fast</option>
                    <option value="3200" selected>Normal</option>
                    <option value="4500">Slow</option>
                  </select>
                </label>
              </div>
              <div class="mr-demo-tour-actions">
                <button id="mr-demo-tour-back" class="mr-button mr-button-secondary" type="button" disabled>Back</button>
                <button id="mr-demo-tour-start" class="mr-button mr-button-primary" type="button">Start walkthrough</button>
                <button id="mr-demo-tour-next" class="mr-button mr-button-ghost" type="button">Next</button>
                <button id="mr-demo-tour-auto" class="mr-button mr-button-ghost" type="button">Autoplay tour</button>
                <button id="mr-demo-tour-stop" class="mr-button mr-button-secondary" type="button" disabled>Exit walkthrough</button>
              </div>
            </div>
          </section>
          <div id="mr-cluster-stage" class="mr-visually-hidden">Cluster diagram starts here</div>
          <div id="mr-cluster" class="mr-cluster" role="img" aria-label="Cluster diagram showing the current leader, followers, learners, and link health">
            <svg id="mr-demo-links" viewBox="0 0 760 360" preserveAspectRatio="none"></svg>
            <div id="mr-demo-particles" class="mr-particle-layer" aria-hidden="true"></div>
          </div>
          <div class="mr-demo-message-strip">
            <div class="mr-message-card">
              <strong>Selected command</strong>
              <span id="mr-demo-command-view">set value=42</span>
            </div>
            <div class="mr-message-card">
              <strong>Quorum status</strong>
              <span id="mr-demo-quorum-badge" class="mr-quorum-badge is-healthy">Healthy quorum</span>
              <span id="mr-demo-quorum">3/3 raft nodes available</span>
            </div>
            <div class="mr-message-card">
              <strong>Commit result</strong>
              <span id="mr-demo-outcome" aria-live="polite">Ready to replicate</span>
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
          <div class="mr-demo-state-machine">
            <div class="mr-demo-state-machine-head">
              <div>
                <p class="mr-demo-panel-kicker">Atomic register</p>
                <h3 class="mr-demo-state-machine-title">Current state</h3>
              </div>
              <span id="mr-demo-register-value" class="mr-demo-state-machine-value">empty</span>
            </div>
            <ol id="mr-demo-command-history" class="mr-demo-command-history"></ol>
          </div>
          <div class="mr-demo-panel-card mr-demo-panel-card-emphasis mr-demo-status-card" aria-labelledby="mr-demo-status-title">
            <div class="mr-demo-panel-mode-head">
              <div>
                <p class="mr-demo-panel-kicker">Status and explanation</p>
                <h2 id="mr-demo-status-title" class="mr-demo-panel-title">How to read this cluster state</h2>
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
              Beginner mode keeps the page focused on the mental model you need before opening Setup or the tutorial. Switch to Deep dive for the raw trace.
            </p>
            <div class="mr-demo-status-flow">
              <section class="mr-demo-status-section">
                <p class="mr-demo-panel-kicker mr-demo-panel-kicker-inline">Current state</p>
                <h3 id="mr-demo-state-title" class="mr-demo-state-title" aria-live="polite">Local 3-node group, ready for a write</h3>
                <p id="mr-demo-state-body" class="mr-demo-state-body" aria-live="polite">
                  Start with one normal write, then isolate node-c to see that progress depends on majority.
                </p>
              </section>
              <section class="mr-demo-status-section mr-demo-status-section-action">
                <p class="mr-demo-panel-kicker mr-demo-panel-kicker-inline">Recommended next action</p>
                <h3 id="mr-demo-next-action-title" class="mr-demo-state-title">Run the first replicated write</h3>
                <p id="mr-demo-next-action-body" class="mr-demo-state-body" aria-live="polite">
                  Start with the healthy write preset.
                </p>
              </section>
            </div>
            <div class="mr-demo-explainer">
              <p id="mr-demo-explainer-title" class="mr-demo-explainer-title">This mirrors the local tutorial harness.</p>
              <p id="mr-demo-explainer-body" class="mr-demo-explainer-body">A healthy leader accepts the command, replicates it, and commits once quorum acknowledges it.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="mr-demo-detail-grid">
      <section class="mr-demo-panel-card mr-demo-panel-card-flat mr-demo-panel-card-deep" data-mr-demo-view="deep" hidden>
        <div class="mr-demo-deep-head">
          <div>
            <p class="mr-demo-panel-kicker">Inspect mode</p>
            <h3 class="mr-demo-panel-title">Trace the cluster step by step</h3>
          </div>
          <div class="mr-demo-share-row">
              <button class="mr-button mr-button-ghost mr-demo-share-button" type="button" data-mr-demo-share>
                <span class="fa-solid fa-link" aria-hidden="true"></span>
                <span class="mr-demo-share-label">Copy state link</span>
              </button>
          </div>
        </div>
        <p class="mr-demo-scenario-status">Use a preset, then read the trace, safety notes, and event log in that order.</p>
        <div class="mr-demo-deep-grid">
          <section class="mr-demo-deep-section mr-demo-deep-section-scenarios">
            <p class="mr-demo-panel-kicker">Jump to a state</p>
            <h4 class="mr-demo-deep-title">Preset scenarios</h4>
            <div class="mr-scenario-list">
              <button id="mr-demo-scenario-happy-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">1</span>
                <strong>Happy path</strong>
                <span>Leader replicates a new command to quorum.</span>
              </button>
              <button id="mr-demo-scenario-partition-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">2</span>
                <strong>Follower partition</strong>
                <span>One follower drops, quorum still commits.</span>
              </button>
              <button id="mr-demo-scenario-recovery-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">3</span>
                <strong>Recovery</strong>
                <span>Isolated node reconnects and catches up.</span>
              </button>
              <button id="mr-demo-scenario-snapshot-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">4</span>
                <strong>Snapshot catch-up</strong>
                <span>A lagging follower installs a snapshot and rejoins.</span>
              </button>
              <button id="mr-demo-scenario-failover-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">5</span>
                <strong>Leader failover</strong>
                <span>The leader disappears and the remaining majority elects a new one.</span>
              </button>
              <button id="mr-demo-scenario-backpressure-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">6</span>
                <strong>Backpressure</strong>
                <span>The leader is healthy but rejects a new write under load.</span>
              </button>
              <button id="mr-demo-scenario-learner-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">7</span>
                <strong>Add learner</strong>
                <span>A new non-voting node joins and leaves quorum unchanged.</span>
              </button>
              <button id="mr-demo-scenario-promote-learner-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">8</span>
                <strong>Promote learner</strong>
                <span>The learner is promoted and quorum grows to 3-of-4 voters.</span>
              </button>
              <button id="mr-demo-scenario-four-node-quorum-deep" class="mr-scenario" type="button">
                <span class="mr-scenario-order">9</span>
                <strong>3-of-4 quorum</strong>
                <span>After promotion, one follower is gone and three voting nodes still commit.</span>
              </button>
            </div>
          </section>
          <section class="mr-demo-deep-section mr-demo-deep-section-report">
            <p class="mr-demo-panel-kicker">Monitoring docs</p>
            <h4 class="mr-demo-deep-title">Cluster report</h4>
            <p class="mr-muted">A compact view of the same term, leader, commit index, and quorum signals you would inspect through <code>RaftNodeReport</code>.</p>
            <dl id="mr-demo-report" class="mr-demo-report"></dl>
          </section>
          <section class="mr-demo-deep-section mr-demo-deep-section-trace">
            <p class="mr-demo-panel-kicker">Inspect mode</p>
            <h4 class="mr-demo-deep-title">Change trace</h4>
            <ol id="mr-demo-timeline" class="mr-timeline"></ol>
          </section>
          <section class="mr-demo-deep-section mr-demo-deep-section-safety">
            <p class="mr-demo-panel-kicker">Inspect mode</p>
            <h4 class="mr-demo-deep-title">Safety notes</h4>
            <ol id="mr-demo-flow" class="mr-flow-list"></ol>
          </section>
          <section class="mr-demo-deep-section mr-demo-deep-section-wide">
            <p class="mr-demo-panel-kicker">Inspect mode</p>
            <h4 class="mr-demo-deep-title">Event log</h4>
            <p class="mr-muted">Use this to correlate each action with the cluster changes above.</p>
            <ul id="mr-demo-log" class="mr-log"></ul>
          </section>
        </div>
      </section>
    </div>
  </div>
</div>

## Map the demo to the real code

<div class="mr-doc-grid mr-resource-grid">
  <a class="mr-doc-card mr-resource-card" href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/LeaderElectionTest.java">
    <strong class="mr-resource-card-title">Leader election</strong>
    <span class="mr-resource-card-summary">The leader rotation idea in this demo maps to the runnable tutorial test below.</span>
    <span class="mr-card-cta mr-card-cta-github"><span class="fa-brands fa-github" aria-hidden="true"></span><span>View source</span></span>
  </a>
  <a class="mr-doc-card mr-resource-card" href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/OperationCommitTest.java">
    <strong class="mr-resource-card-title">Log replication</strong>
    <span class="mr-resource-card-summary">The "Replicate Command" flow corresponds to the operation commit scenario.</span>
    <span class="mr-card-cta mr-card-cta-github"><span class="fa-brands fa-github" aria-hidden="true"></span><span>View source</span></span>
  </a>
  <a class="mr-doc-card mr-resource-card" href="https://github.com/MicroRaft/MicroRaft/tree/master/microraft-tutorial/src/main/java/io/microraft/tutorial/atomicregister">
    <strong class="mr-resource-card-title">State machine</strong>
    <span class="mr-resource-card-summary">The demo commands represent the small atomic register state machine used in the tutorial module.</span>
    <span class="mr-card-cta mr-card-cta-github"><span class="fa-brands fa-github" aria-hidden="true"></span><span>View source</span></span>
  </a>
  <a class="mr-doc-card mr-resource-card" href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/BaseLocalTest.java">
    <strong class="mr-resource-card-title">Local cluster wiring</strong>
    <span class="mr-resource-card-summary">The in-browser nodes are a teaching abstraction; the local transport and test harness show the real in-process setup.</span>
    <span class="mr-card-cta mr-card-cta-github"><span class="fa-brands fa-github" aria-hidden="true"></span><span>View source</span></span>
  </a>
</div>

## What this demo is for

- Build intuition for leader election, quorum, replication, and recovery before reading code.
- Give new users the smallest mental model before they run the local tutorial.
- Show how the atomic register tutorial behaves without asking you to read the whole docs set first.

## What to do next

- Want the fastest runnable command? Go to [Setup](docs/setup.md).
- Want the integration vocabulary first? Read [Main Abstractions](docs/main-abstractions.md).
- Want the real end-to-end implementation? Read [Tutorial: Building an Atomic Register](docs/tutorial-building-an-atomic-register.md).
- Want the positioning story first? Read [Why MicroRaft?](docs/why-microraft.md).
