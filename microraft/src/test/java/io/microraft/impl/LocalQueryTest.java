/*
 * Original work Copyright (c) 2008-2020, Hazelcast, Inc.
 * Modified work Copyright 2020, MicroRaft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.microraft.impl;

import io.microraft.Ordered;
import io.microraft.RaftEndpoint;
import io.microraft.exception.LaggingCommitIndexException;
import io.microraft.exception.NotLeaderException;
import io.microraft.impl.local.LocalRaftGroup;
import io.microraft.impl.util.BaseTest;
import io.microraft.model.message.AppendEntriesRequest;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static io.microraft.QueryPolicy.ANY_LOCAL;
import static io.microraft.QueryPolicy.LEADER_LOCAL;
import static io.microraft.impl.local.SimpleStateMachine.apply;
import static io.microraft.impl.local.SimpleStateMachine.query;
import static io.microraft.impl.util.AssertionUtils.eventually;
import static io.microraft.impl.util.RaftTestUtils.getCommitIndex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author mdogan
 * @author metanet
 */
public class LocalQueryTest
        extends BaseTest {

    private LocalRaftGroup group;

    @After
    public void destroy() {
        if (group != null) {
            group.destroy();
        }
    }

    @Test(timeout = 300_000)
    public void when_queryFromLeader_withoutAnyCommit_thenReturnDefaultValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        Ordered<Object> o = leader.query(query(), LEADER_LOCAL, 0).get();
        assertThat(o.getResult()).isNull();
        assertThat(o.getCommitIndex()).isEqualTo(0);
    }

    @Test(timeout = 300_000)
    public void when_queryFromLeaderWithCommitIndex_withoutAnyCommit_thenFail()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        try {
            leader.query(query(), LEADER_LOCAL, getCommitIndex(leader) + 1).get();
            fail();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseInstanceOf(LaggingCommitIndexException.class);
        }
    }

    @Test(timeout = 300_000)
    public void when_queryFromFollower_withoutAnyCommit_thenReturnDefaultValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        group.waitUntilLeaderElected();
        RaftNodeImpl follower = group.getAnyFollowerNode();

        Ordered<Object> o = follower.query(query(), ANY_LOCAL, 0).get();
        assertThat(o.getResult()).isNull();
        assertThat(o.getCommitIndex()).isEqualTo(0);
    }

    @Test(timeout = 300_000)
    public void when_queryFromFollowerWithCommitIndex_withoutAnyCommit_thenReturnDefaultValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        group.waitUntilLeaderElected();
        RaftNodeImpl follower = group.getAnyFollowerNode();

        try {
            follower.query(query(), ANY_LOCAL, getCommitIndex(follower) + 1).get();
            fail();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseInstanceOf(LaggingCommitIndexException.class);
        }
    }

    @Test(timeout = 300_000)
    public void when_queryFromLeaderWithoutCommitIndex_onStableCluster_thenReadLatestValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        int count = 3;
        for (int i = 1; i <= count; i++) {
            leader.replicate(apply("value" + i)).get();
        }

        long commitIndex = getCommitIndex(leader);
        Ordered<Object> result = leader.query(query(), LEADER_LOCAL, 0).get();
        assertThat(result.getResult()).isEqualTo("value" + count);
        assertThat(result.getCommitIndex()).isEqualTo(commitIndex);
    }

    @Test(timeout = 300_000)
    public void when_queryFromLeaderWithCommitIndex_onStableCluster_thenReadLatestValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        int count = 3;
        for (int i = 1; i <= count; i++) {
            leader.replicate(apply("value" + i)).get();
        }

        long commitIndex = getCommitIndex(leader);
        Ordered<Object> result = leader.query(query(), LEADER_LOCAL, commitIndex).get();
        assertThat(result.getResult()).isEqualTo("value" + count);
        assertThat(result.getCommitIndex()).isEqualTo(commitIndex);
    }

    @Test(timeout = 300_000)
    public void when_queryFromLeaderWithFurtherCommitIndex_onStableCluster_thenFail()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        int count = 3;
        for (int i = 1; i <= count; i++) {
            leader.replicate(apply("value" + i)).get();
        }

        long commitIndex = getCommitIndex(leader);
        try {
            leader.query(query(), LEADER_LOCAL, commitIndex + 1).get();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseInstanceOf(LaggingCommitIndexException.class);
        }
    }

    @Test(timeout = 300_000)
    public void when_queryFromFollower_withLeaderLocalPolicy_thenFail()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();
        leader.replicate(apply("value")).get();

        try {
            group.getAnyFollowerNode().query(query(), LEADER_LOCAL, 0).get();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseInstanceOf(NotLeaderException.class);
        }
    }

    @Test(timeout = 300_000)
    public void when_queryFromFollowerWithoutCommitIndex_onStableCluster_thenReadLatestValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        int count = 3;
        for (int i = 1; i <= count; i++) {
            leader.replicate(apply("value" + i)).get();
        }

        String latestValue = "value" + count;

        eventually(() -> {
            long commitIndex = getCommitIndex(leader);
            for (RaftNodeImpl follower : group.getNodesExcept(leader.getLocalEndpoint())) {
                assertThat(getCommitIndex(follower)).isEqualTo(commitIndex);
                Ordered<Object> result = follower.query(query(), ANY_LOCAL, 0).get();
                assertThat(result.getResult()).isEqualTo(latestValue);
                assertThat(result.getCommitIndex()).isEqualTo(commitIndex);
            }
        });
    }

    @Test(timeout = 300_000)
    public void when_queryFromFollowerWithCommitIndex_onStableCluster_thenReadLatestValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        int count = 3;
        for (int i = 1; i <= count; i++) {
            leader.replicate(apply("value" + i)).get();
        }

        String latestValue = "value" + count;

        eventually(() -> {
            long commitIndex = getCommitIndex(leader);
            for (RaftNodeImpl follower : group.getNodesExcept(leader.getLocalEndpoint())) {
                assertThat(getCommitIndex(follower)).isEqualTo(commitIndex);
                Ordered<Object> result = follower.query(query(), ANY_LOCAL, commitIndex).get();
                assertThat(result.getResult()).isEqualTo(latestValue);
                assertThat(result.getCommitIndex()).isEqualTo(commitIndex);
            }
        });
    }

    @Test(timeout = 300_000)
    public void when_queryFromSlowFollower_thenReadStaleValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();
        RaftNodeImpl slowFollower = group.getAnyFollowerNode();

        Object firstValue = "value1";
        leader.replicate(apply(firstValue)).get();
        long leaderCommitIndex = getCommitIndex(leader);

        eventually(() -> assertThat(getCommitIndex(slowFollower)).isEqualTo(leaderCommitIndex));

        group.dropMessagesToMember(leader.getLocalEndpoint(), slowFollower.getLocalEndpoint(), AppendEntriesRequest.class);

        leader.replicate(apply("value2")).get();

        Ordered<Object> result = slowFollower.query(query(), ANY_LOCAL, 0).get();
        assertThat(result.getResult()).isEqualTo(firstValue);
        assertThat(result.getCommitIndex()).isEqualTo(leaderCommitIndex);
    }

    @Test(timeout = 300_000)
    public void when_queryFromSlowFollower_thenEventuallyReadLatestValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();
        leader.replicate(apply("value1")).get();

        RaftNodeImpl slowFollower = group.getAnyFollowerNode();
        group.dropMessagesToMember(leader.getLocalEndpoint(), slowFollower.getLocalEndpoint(), AppendEntriesRequest.class);

        Object lastValue = "value2";
        leader.replicate(apply(lastValue)).get();

        group.allowAllMessagesToMember(leader.getLocalEndpoint(), slowFollower.getLocalEndpoint());

        eventually(() -> {
            long commitIndex = getCommitIndex(leader);
            Ordered<Object> result = slowFollower.query(query(), ANY_LOCAL, 0).get();
            assertThat(result.getResult()).isEqualTo(lastValue);
            assertThat(result.getCommitIndex()).isEqualTo(commitIndex);
        });
    }

    @Test(timeout = 300_000)
    public void when_queryFromSplitLeaderWithAnyLocal_thenReadStaleValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        Object firstValue = "value1";
        leader.replicate(apply(firstValue)).get();
        long firstCommitIndex = getCommitIndex(leader);

        eventually(() -> {
            for (RaftNodeImpl node : group.getNodes()) {
                assertThat(getCommitIndex(node)).isEqualTo(firstCommitIndex);
            }
        });

        RaftNodeImpl followerNode = group.getAnyFollowerNode();
        group.splitMembers(leader.getLocalEndpoint());

        eventually(() -> {
            RaftEndpoint leaderEndpoint = followerNode.getLeaderEndpoint();
            assertThat(leaderEndpoint).isNotNull().isNotEqualTo(leader.getLocalEndpoint());
        });

        RaftNodeImpl newLeader = group.getNode(followerNode.getLeaderEndpoint());
        Object lastValue = "value2";
        newLeader.replicate(apply(lastValue)).get();
        long lastCommitIndex = getCommitIndex(newLeader);

        Ordered<Object> result1 = newLeader.query(query(), ANY_LOCAL, 0).get();
        assertThat(result1.getResult()).isEqualTo(lastValue);
        assertThat(result1.getCommitIndex()).isEqualTo(lastCommitIndex);

        Ordered<Object> result2 = leader.query(query(), ANY_LOCAL, 0).get();
        assertThat(result2.getResult()).isEqualTo(firstValue);
        assertThat(result2.getCommitIndex()).isEqualTo(firstCommitIndex);
    }

    @Test(timeout = 300_000)
    public void when_queryFromSplitLeaderWithLeaderLocal_then_readFailsAfterLeaderDemotesToFollower()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        Object firstValue = "value1";
        leader.replicate(apply(firstValue)).get();
        long firstCommitIndex = getCommitIndex(leader);

        eventually(() -> {
            for (RaftNodeImpl node : group.getNodes()) {
                assertThat(getCommitIndex(node)).isEqualTo(firstCommitIndex);
            }
        });

        group.splitMembers(leader.getLocalEndpoint());

        eventually(() -> {
            try {
                leader.query(query(), LEADER_LOCAL, 0).get();
                fail();
            } catch (ExecutionException e) {
                assertThat(e).hasCauseInstanceOf(NotLeaderException.class);
            }
        });
    }

    @Test(timeout = 300_000)
    public void when_queryFromSplitLeader_thenEventuallyReadLatestValue()
            throws Exception {
        group = new LocalRaftGroup(3);
        group.start();

        RaftNodeImpl leader = group.waitUntilLeaderElected();

        Object firstValue = "value1";
        leader.replicate(apply(firstValue)).get();
        long leaderCommitIndex = getCommitIndex(leader);

        eventually(() -> {
            for (RaftNodeImpl node : group.getNodes()) {
                assertThat(getCommitIndex(node)).isEqualTo(leaderCommitIndex);
            }
        });

        RaftNodeImpl followerNode = group.getAnyFollowerNode();
        group.splitMembers(leader.getLocalEndpoint());

        eventually(() -> {
            RaftEndpoint leaderEndpoint = followerNode.getLeaderEndpoint();
            assertThat(leaderEndpoint).isNotNull().isNotEqualTo(leader.getLocalEndpoint());
        });

        RaftNodeImpl newLeader = group.getNode(followerNode.getLeaderEndpoint());
        Object lastValue = "value2";
        newLeader.replicate(apply(lastValue)).get();
        long lastCommitIndex = getCommitIndex(newLeader);

        group.merge();

        eventually(() -> {
            Ordered<Object> result = leader.query(query(), ANY_LOCAL, 0).get();
            assertThat(result.getResult()).isEqualTo(lastValue);
            assertThat(result.getCommitIndex()).isEqualTo(lastCommitIndex);
        });
    }

}