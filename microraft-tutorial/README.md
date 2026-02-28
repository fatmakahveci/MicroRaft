# MicroRaft Tutorial

This module is the fastest way to see MicroRaft running locally. It contains a
small in-process transport, a sample atomic register state machine, and a set
of focused tests that each demonstrate one important Raft behavior.

## Quick Start

Run the end-to-end operation commit scenario:

```bash
./gradlew :microraft-tutorial:test \
  --tests io.microraft.tutorial.OperationCommitTest \
  -Pmicroraft.javaVersion=20
```

If Java 11 is already installed locally, you can omit
`-Pmicroraft.javaVersion=20` because the build defaults to Java 11.

What this test does:

- starts a local 3-node Raft group,
- waits for leader election,
- replicates write and compare-and-set operations,
- reads the final value back from the atomic register.

What you should expect:

- the build completes successfully,
- the test prints commit indexes in increasing order,
- the final read returns `value3`.

## Other Useful Scenarios

Use these tests when you want to inspect a specific behavior:

- `io.microraft.tutorial.LeaderElectionTest`
- `io.microraft.tutorial.LinearizableQueryTest`
- `io.microraft.tutorial.MonotonicLocalQueryTest`
- `io.microraft.tutorial.SnapshotInstallationTest`
- `io.microraft.tutorial.ChangeRaftGroupMemberListTest`

Example:

```bash
./gradlew :microraft-tutorial:test \
  --tests io.microraft.tutorial.LeaderElectionTest \
  -Pmicroraft.javaVersion=20
```

## Start Reading Here

- [OperationCommitTest](src/test/java/io/microraft/tutorial/OperationCommitTest.java) is the best first example.
- [LeaderElectionTest](src/test/java/io/microraft/tutorial/LeaderElectionTest.java) is the shortest possible walkthrough.
- [BaseLocalTest](src/test/java/io/microraft/tutorial/BaseLocalTest.java) shows how the local cluster is bootstrapped.
- [atomicregister](src/main/java/io/microraft/tutorial/atomicregister) contains the sample state machine used by the tutorial.

## Troubleshooting

If the test fails before running:

- install Java 11, or rerun with `-Pmicroraft.javaVersion=<your-installed-version>`,
- use `./gradlew`, not `mvnw`; this repository builds with Gradle,
- if you are only checking whether the cluster forms, start with `LeaderElectionTest`.

For broader setup help, see the root [README](../README.md) and the website
[troubleshooting guide](https://microraft.io/docs/troubleshooting/).
