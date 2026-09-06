---
tab_title: "Resiliency"
seo_title: "MicroRaft Resiliency and Fault Tolerance for Java Raft Clusters"
description: "Study MicroRaft resiliency under high load, leader loss, minority failure, recovery, backpressure, and other Raft fault scenarios."
keywords: "MicroRaft resiliency, Java Raft fault tolerance, leader failure Java, quorum loss Raft, backpressure Raft, recovery behavior"
schema_type: TechArticle
og_type: article
doc_layout: article
---
<div class="mr-doc-shell">
  <section class="mr-doc-hero">
    <h1 class="mr-page-title mr-doc-title">Resiliency and Fault Tolerance</h1>
    <p class="mr-page-summary">
      This page is about failure behavior, not feature marketing. It walks
      through the cases that actually decide whether a Raft-backed subsystem is
      operable under stress.
    </p>
  </section>

  <section class="mr-doc-grid">
    <article class="mr-doc-card">
      <h3>Core rule</h3>
      <p>
        Safety holds as long as there is no Byzantine behavior. Availability
        depends on majority health and communication.
      </p>
    </article>
    <article class="mr-doc-card">
      <h3>What you will see</h3>
      <ul class="mr-doc-list">
        <li>high load and backpressure</li>
        <li>minority and majority failures</li>
        <li>leader failure and retry semantics</li>
        <li>recovery with and without persistence</li>
      </ul>
    </article>
    <article class="mr-doc-card">
      <h3>Good companion pages</h3>
      <ul class="mr-doc-list">
        <li><a href="/docs/configuration/">Configuration</a></li>
        <li><a href="/docs/monitoring/">Monitoring</a></li>
        <li><a href="/docs/troubleshooting/">Troubleshooting</a></li>
      </ul>
    </article>
  </section>
</div>

In this section, we will walk through different types of failure scenarios and
discuss how MicroRaft handles each one of them. We will use MicroRaft's
[local testing utilities](https://github.com/MicroRaft/MicroRaft/tree/master/microraft/src/test/java/io/microraft/impl/local)
to demonstrate those failure
scenarios. These utilities are mainly used for testing MicroRaft to a great
extent without a distributed setting. Here, we will use them to run a Raft group
in a single JVM process and inject different types of failures into the system.

<div class="mr-callout mr-callout-info">
  <div class="mr-callout-title">Safety baseline</div>
  <p>Operations remain committed in a single global order and are not lost as long as there is no Byzantine failure. In MicroRaft, restarting a node with the same identity but without valid persisted state is an example of the kind of behavior that breaks that assumption.</p>
</div>

The availability of a Raft group mainly depends on if the majority (i.e., more
than half) of the Raft nodes are alive and able to communicate with each other.
The main rule is, `2f + 1` Raft nodes tolerate failure of `f` Raft nodes. For
instance, a 3-node Raft group can tolerate failure of 1 Raft node, or a 5-node
Raft group can tolerate failure of 2 Raft nodes without losing availability.

<div class="mr-doc-grid">
  <article class="mr-doc-card">
    <h3>Read this page in three questions</h3>
    <ul class="mr-doc-list">
      <li>what happens under load?</li>
      <li>what happens when a minority or leader fails?</li>
      <li>what failures are unrecoverable without restoring majority?</li>
    </ul>
  </article>
  <article class="mr-doc-card">
    <h3>Operational lens</h3>
    <ul class="mr-doc-list">
      <li>safety and availability are different questions</li>
      <li>persistence changes your recovery options materially</li>
      <li>client retry behavior is part of the system design</li>
    </ul>
  </article>
</div>

## 1. Handling high system load

Even if the majority of a Raft group is alive, we may encounter unavailability
issues if the Raft group is under high load and cannot keep up with the request
rate. In this case, the leader Raft node temporarily stops accepting new
requests and notifies the futures returned from the `RaftNode` methods with
[`CannotReplicateException`](https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/main/java/io/microraft/exception/CannotReplicateException.java).
This exception means that there
are too many operations pending to be committed in the leader's local Raft log,
or too many queries pending to be executed, so it temporarily rejects accepting
new requests. Clients should apply some backoff before retrying their requests.

We will demonstrate this scenario in a test below with a 3-node Raft group. In
MicroRaft, a leader does not replicate log entries one by one. Instead, it keeps
a buffer for incoming requests and replicates the log entries to the followers
in batches in order to improve the throughput. Once this buffer is filled up,
the leader stops accepting new requests. In this test, we allow the pending log
entries buffer to keep at most 10 requests. We also slow down our followers
synthetically by making them sleep for 3 seconds. Then, we start sending
requests to the leader. After some time, our requests fail with
`CannotReplicateException`.

<div class="mr-snippet-note">
  <strong>What this snippet proves</strong>
  <p>The leader should apply backpressure instead of pretending it can buffer unbounded work. Under sustained load, failure is explicit and therefore operable.</p>
</div>

<script src="https://gist.github.com/metanet/3350f8107c01171f46bf08644cec582c.js"></script>

To run this test on your machine, try the following:

~~~~{.bash}
$ ./gradlew :microraft:test --tests io.microraft.faulttolerance.HighLoadTest
~~~~

You can also see it in the
[MicroRaft GitHub repository](https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/test/java/io/microraft/faulttolerance/HighLoadTest.java).

<div class="mr-doc-grid">
  <article class="mr-doc-card">
    <h3>Takeaway from high load</h3>
    <p>
      Backpressure is a feature, not a bug. If the leader cannot safely keep up,
      it should reject new work instead of pretending throughput is infinite.
    </p>
  </article>
  <article class="mr-doc-card">
    <h3>What to tune next</h3>
    <ul class="mr-doc-list">
      <li>pending log entry count</li>
      <li>append entries batch size</li>
      <li>client retry and backoff policy</li>
    </ul>
  </article>
</div>

<div class="mr-section-break"><span>Minority Failure</span></div>

## 2. Minority failure

Failure of the minority (i.e, less than half) may cause the Raft group to lose
availability temporarily, but eventually the Raft group continues to accept and
commit new requests. If we have a persistence implementation (i.e, `RaftStore`),
we can recover failed Raft nodes. On the other hand, if we don't have
persistence or cannot recover the persisted Raft data, we can remove failed Raft
nodes from the Raft group. Please note that when we remove a Raft node from a
Raft group, the majority quorum size is re-calculated based on the new size of
the Raft group. In order to replace a non-recoverable Raft node without hurting
the overall availability of the Raft group, we should remove the crashed Raft
node first and then add a fresh-new one.

<div class="mr-callout mr-callout-warning">
  <div class="mr-callout-title">Warning</div>
  <p>If nodes start without a real <code>RaftStore</code>, restarting a crashed node with the same identity breaks Raft safety. Without persistence, the only safe recovery path is to remove the failed node while majority is still alive.</p>
</div>

To restart a crashed or terminated Raft node, we can read its persisted state
into a `RestoredRaftState` object. Then, we can use this object to restore the
Raft node back. __Please note that terminating a Raft node manually without a
persistence layer implementation is equivalent to a crash since there is no way
to restore the Raft node back with its Raft state.__

MicroRaft provides a basic in-memory `RaftStore` implementation to enable
crash-recovery testing. In the following code sample, we use this utility, i.e.,
`InMemoryRaftStore`, to demonstrate how to recover from Raft node failures.

<div class="mr-snippet-note">
  <strong>What this snippet proves</strong>
  <p>Minority loss is recoverable when persisted Raft state exists. The snippet demonstrates that restored nodes can rejoin safely and replay committed state.</p>
</div>

<script src="https://gist.github.com/metanet/14e9ef6d9a5f3992a03de5cd8a874589.js"></script>

To run this test on your machine, try the following:

~~~~{.bash}
$ ./gradlew :microraft:test --tests io.microraft.faulttolerance.RestoreCrashedRaftNodeTest
~~~~

You can also see it in the
[MicroRaft GitHub repository](https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/test/java/io/microraft/faulttolerance/RestoreCrashedRaftNodeTest.java).

This time we provide a factory object to enable `LocalRaftGroup` to create
`InMemoryRaftStore` objects while configuring our Raft nodes. Hence, after we
terminate our Raft nodes, we will be able to read their persisted state. Once we
start the Raft group, we commit a value via the leader, observe that value with
a local query on a follower, and crash a follower. Then, we read its persisted
state via our `InMemoryRaftStore` object and restore the follower back. Please
ignore the details of `RaftTestUtils.getRestoredState()` and
`RaftTestUtils.getRaftStore()`. Once the follower starts running again, it talks
to the other Raft nodes, discovers the current leader Raft node and its commit
index, and replays all committed operations on its state machine.

Our `sysout` lines in this test print the following:

~~~~{.text}
replicate result: value, commit index: 1
monotonic local query successful on follower. query result: value, commit index: 1
monotonic local query successful on restarted follower. query result: value, commit index: 1
~~~~

<div class="mr-callout mr-callout-warning">
  <div class="mr-callout-title">Replay caveat</div>
  <p>When a node starts from restored state, it replays committed log entries to rebuild the state machine. If your operations have side effects, replay can trigger them again unless the state machine is designed for it. See <a href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/main/java/io/microraft/statemachine/StateMachine.java" target="_blank" rel="noopener noreferrer">StateMachine</a> for the boundary.</p>
</div>

<div class="mr-doc-grid">
  <article class="mr-doc-card">
    <h3>Takeaway from minority failure</h3>
    <p>
      Minority loss is survivable, but your exact recovery path depends on
      whether persisted state exists and can be trusted.
    </p>
  </article>
  <article class="mr-doc-card">
    <h3>Do not miss this</h3>
    <ul class="mr-doc-list">
      <li>restarting a node without real persistence can break safety</li>
      <li>restored logs replay side effects unless your state machine accounts for them</li>
    </ul>
  </article>
</div>

<div class="mr-section-break"><span>Leader Failure</span></div>

## 3. Raft leader failure

When a leader Raft node fails, the Raft group temporarily loses availability
until the other Raft nodes notice the failure and elect a new leader. Delay of
the detection of the leader's failure depends on the _leader heartbeat timeout_
configuration. Please refer to the [Configuration
section](configuration.md) to learn more about the _leader election
timeout_ and _leader heartbeat timeout_ configuration parameters.

If a client notices that the current leader is not responding, it can contact
other Raft nodes in the Raft group in a round-robin fashion and query the leader
via the `RaftNode.getReport()` API. If the leader actually crashes, the
followers eventually notice its failure and elect a new leader. Then, our client
will be able to discover the new leader Raft endpoint via this API. However, if
a client cannot communicate with an alive leader because of an environmental
issue, such as a network problem, it cannot replicate new operations, or run
`QueryPolicy.LINEARIZABLE` and `QueryPolicy.LEADER_LOCAL` queries. It means that
the Raft group is unavailable for this particular client. This is due to
MicroRaft's simplicity-oriented design philosophy. In MicroRaft, when a follower
Raft node receives an API call that requires the leadership role, it does not
internally forward the call to the leader Raft node. Instead, it fails the call
with `NotLeaderException`. Please note that this mechanism can be also used for
leader discovery. When a client needs to discover the leader, it can try talking
to any Raft node. If its call fails with `NotLeaderException`, the client can
check if the exception points the current leader Raft endpoint via
`NotLeaderException.getLeader()`. Otherwise, it can try the same with another
Raft node.

If a Raft leader crashes before a client receives response for an operation
passed to `RaftNode.replicate()`, there are multiple possibilities:

- If the leader failed before replicating the operation to any follower, then
  the operation certainly won't be committed.

- If the failed leader replicated the operation to at least one follower, then
  the operation might be committed if a follower having that operation becomes
  leader. However, another follower could become the new leader and overwrite
  that operation if it was not replicated to the majority by the crashed leader.

- The good thing about queries is, they are idempotent. Clients can safely retry
  their queries on the new leader.

<div class="mr-callout mr-callout-warning">
  <div class="mr-callout-title">Retry caveat</div>
  <p>Clients own retry behavior when a write result is ambiguous. A retry may commit the same logical operation twice. MicroRaft intentionally does not add built-in deduplication here; if you need it, implement it in your <code>StateMachine</code>.</p>
</div>

We will see the second scenario described above in a code sample. In the
following test, we replicate an operation via the Raft leader, but block the
responses sent back from the followers. Even though the leader managed to
replicate our operation to the majority, it is not able to commit it because it
couldn't learn that the followers also appended this operation to their logs. At
this step, we crash the leader. We won't get any response for our operation now
since the leader is gone, so we will just re-replicate it with the new leader.
The thing is, the previous leader managed to replicate our first operation to
the majority, so the new leader will commit it. Since we replicate it for the
second time with the new leader, we cause a duplicate commit. When we query the
new leader, we see that there are 2 values applied to the state machine.

<div class="mr-snippet-note">
  <strong>What this snippet proves</strong>
  <p>Leader failure turns protocol correctness into a client semantics problem. A write can become ambiguous, so retries must be designed for duplicate effects.</p>
</div>

<script src="https://gist.github.com/metanet/125d33a0e009e9119f5c9a96061ec69e.js"></script>

To run this test on your machine, try the following:

~~~~{.bash}
$ ./gradlew :microraft:test --tests io.microraft.faulttolerance.RaftLeaderFailureTest
~~~~

You can also see it in the
[MicroRaft GitHub repository](https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/test/java/io/microraft/faulttolerance/RaftLeaderFailureTest.java).

<div class="mr-callout mr-callout-info">
  <div class="mr-callout-title">Practical pattern</div>
  <p>Idempotent operations make leader-failure retries far safer because duplicate commits become tolerable. That is often a good design target, even though not every workload can express its writes that way.</p>
</div>

<div class="mr-doc-grid">
  <article class="mr-doc-card">
    <h3>Takeaway from leader failure</h3>
    <p>
      Leader failure is mostly a client semantics problem after it becomes a
      protocol problem. Your retry model decides whether ambiguity turns into
      duplicate effects.
    </p>
  </article>
  <article class="mr-doc-card">
    <h3>Design implication</h3>
    <ul class="mr-doc-list">
      <li>prefer idempotent operations where possible</li>
      <li>treat <code>NotLeaderException</code> as a leader discovery signal</li>
      <li>separate retry policy for reads and writes</li>
    </ul>
  </article>
</div>

<div class="mr-section-break"><span>Majority Failure</span></div>

## 4. Majority failure

Failure of the majority causes the Raft group to lose its availability and stop
handling new requests. The only recovery option is to recover some of failed
Raft nodes so that the majority becomes available again. Otherwise, the Raft
group cannot be recovered. MicroRaft does not support any unsafe recovery policy
for now. 

The duration of unavailability depends on how long the majority Raft nodes
remain crashed. Clients won't be able to replicate any new operations or run
linearizable queries in the meantime. However, we can still run local queries
because `QueryPolicy.EVENTUAL_CONSISTENCY` does not require availability of 
the majority.

In MicroRaft, on each heartbeat tick a leader Raft node checks if it is still in
charge, i.e, it has received _Append Entries RPC_ responses from `majority
quorum size - 1` (majority quorum size minus the leader itself) in the last
_leader heartbeat timeout_ period. For instance, in a 3-node Raft group with 5
seconds of _leader heartbeat timeout_, a Raft leader keeps its leadership role
as long as at least 1 follower has sent an _Append Entries RPC_ response in the
last 5 seconds. Otherwise, the leader Raft node demotes itself to the follower
role and fails pending (i.e., locally appended but not yet committed) operations
with [`IndeterminateStateException`](https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/main/java/io/microraft/exception/IndeterminateStateException.java).
This behaviour is due to the
asynchronous nature of distributed systems. When the leader cannot get _Append
Entries RPC_ responses from some of its followers, it may not accurately decide
if those followers are actually crashed, or just temporarily unreachable. If
those unresponsive followers are actually alive and can form the majority, they
can also elect a new leader among themselves and commit operations replicated by
the previous leader. Hence, MicroRaft takes a defensive approach here and makes
a leader Raft node step down from the leadership role.  

<div class="mr-callout mr-callout-warning">
  <div class="mr-callout-title">Indeterminate state</div>
  <p>Clients must decide how to handle <code>IndeterminateStateException</code>. Retrying immediately may duplicate an operation, so write semantics need to be designed with ambiguity in mind.</p>
</div>

We will see another code sample to demonstrate how to restore from majority
failure. In this part we use the `InMemoryRaftStore` utility we used in
`RestoreCrashedRaftNodeTest` above. We start a 3-node Raft group, commit an
operation, and terminate both of our 2 followers. Then, we try to replicate a
new operation. However, in a few seconds the leader will notice that it has not
received _Append Entries RPC_ responses from the majority and step down from the
leadership role. Because of that, it will also fail our operation with
`IndeterminateOperationStateException`. Since it is a follower now, it will
directly reject new `RaftNode.replicate()` calls with `NotLeaderException`. At
this point, our Raft group is unavailable for `RaftNode.replicate()` calls, and
`RaftNode.query()`calls for `QueryPolicy.LINEARIZABLE` and
`QueryPolicy.LEADER_LEASE`, but we can still perform a local query with
`QueryPolicy.EVENTUAL_CONSISTENCY`. If we want to make the Raft group available
again, we don't need to restore all crashed Raft nodes. In this particular
scenario, it is sufficient to restore only 1 Raft node so that we will have
the majority alive again. It is what we do in the last part of the test. Once
we have 2 Raft nodes running again, they will be able to elect a new leader.

In this example, we waited until the leader demotes itself to the follower role
before restarting the crashed Raft nodes. This is not a requirement for
restoring crashed Raft nodes, and we did it here only for the sake of example.
We can restore a crashed Raft node anytime and if the leader Raft node is still
running there may not be a new leader election round and the restarted Raft node
could just discover the leader Raft node. Our Raft group will restore its
availability as long as there is a leader Raft node taking to the majority
(including itself).  

<div class="mr-snippet-note">
  <strong>What this snippet proves</strong>
  <p>When majority is gone, the cluster must stop pretending it is available. Recovery only becomes possible after enough persisted nodes return to reform quorum.</p>
</div>

<script src="https://gist.github.com/metanet/0ab1af1675056445da2cd99295de2665.js"></script>

To run this test on your machine, try the following:

~~~~{.bash}
$ ./gradlew :microraft:test --tests io.microraft.faulttolerance.MajorityFailureTest
~~~~

You can also see it in the
[MicroRaft GitHub repository](https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/test/java/io/microraft/faulttolerance/MajorityFailureTest.java).

<div class="mr-callout mr-callout-warning">
  <div class="mr-callout-title">Persistence required</div>
  <p>This recovery path only works with a real persistence layer. Restarting a crashed node with the same identity but empty state creates a Byzantine-style failure where committed operations can be lost. See <a href="#6-corruption-or-loss-of-persistent-raft-state">Corruption or loss of persistent Raft state</a>.</p>
</div>

<div class="mr-section-break"><span>Network Partitions</span></div>

## 5. Network partitions

Behaviour of a Raft group during a network partition depends on how Raft nodes
are divided to different sides of the network partition, and with which Raft
nodes our clients are interacting with. If any subset of the Raft nodes manage
to form the majority, they remain available. If the Raft leader falls into the
minority side, the Raft nodes in the majority side elect a new leader and
restore their availability.

If our clients cannot talk to the majority side, it means that the Raft group is
unavailable from the perspective of the clients.
    
Similar to the majority failure case described in the previous part, if the
leader Raft node falls into a minority side of the network partition, it demotes
itself to the follower role after the _leader heartbeat timeout_ elapses, and
fails all pending operations with `IndeterminateStateException`. To reiterate,
this exception means that the demoted leader cannot decide if those operations
have been committed or not.
 
When the network problem is resolved, Raft nodes connect to each other again.
The Raft nodes that was on the minority side of the network partition catch up
with the other Raft nodes, and the Raft group continues its normal operation.

<div class="mr-callout mr-callout-info">
  <div class="mr-callout-title">No split brain</div>
  <p>One of the key guarantees in network partition scenarios is the absence of split brain. At most one side can retain a functional leader.</p>
</div>

We will see how our Raft nodes behave in a network partitioning scenario in the
following test. Again, we have a 3-node Raft group here, and we create an
artificial network disconnection between the leader and the followers. Since the
leader cannot talk to the majority anymore, after the _leader heartbeat timeout_
duration elapses, our leader demotes to the follower role. The followers on the
other side elect a new leader among themselves and even commit a new operation.
Once we fix the network problem, we see that the old leader connects back to the
other Raft nodes, discovers the new leader and gets the new committed operation.
Phew! 

<div class="mr-snippet-note">
  <strong>What this snippet proves</strong>
  <p>Partitions do not create split brain here. The minority side steps down, the majority side keeps progress, and the old leader catches up after the network heals.</p>
</div>

<script src="https://gist.github.com/metanet/ac66fbb2f6e2ef5e8224ac387d2e2b44.js"></script>

To run this test on your machine, try the following:

~~~~{.bash}
$ ./gradlew :microraft:test --tests io.microraft.faulttolerance.NetworkPartitionTest
~~~~

You can also see it in the
[MicroRaft GitHub repository](https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/test/java/io/microraft/faulttolerance/NetworkPartitionTest.java).

<div class="mr-section-break"><span>Durability</span></div>

## 6. Corruption or loss of persistent Raft state

If a `RestoredRaftState` object is created with corrupted or partially-restored
Raft state, the safety guarantees of the Raft consensus algorithm no longer
hold. For instance, if a flushed log entry is not present in the
`RestoredRaftState` object, then the restored `RaftNode` may not have a
committed operation. If that Raft node becomes leader, it may commit another
operation for the same log index with the lost operation and breaks the safety
property of the Raft consensus algorithm.

<div class="mr-callout mr-callout-warning">
  <div class="mr-callout-title">Durability contract</div>
  <p><a href="https://github.com/MicroRaft/MicroRaft/blob/master/microraft/src/main/java/io/microraft/persistence/RaftStore.java" target="_blank" rel="noopener noreferrer"><code>RaftStore</code></a> defines the durability and integrity guarantees persistence implementations must uphold. <code>RaftNode</code> assumes restored state is valid; it does not re-verify corrupted or partial state for you.</p>
</div>
