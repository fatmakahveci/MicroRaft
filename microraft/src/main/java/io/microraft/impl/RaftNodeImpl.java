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

import io.microraft.MembershipChangeMode;
import io.microraft.Ordered;
import io.microraft.QueryPolicy;
import io.microraft.RaftConfig;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.RaftNodeStatus;
import io.microraft.exception.CannotReplicateException;
import io.microraft.exception.IndeterminateStateException;
import io.microraft.exception.LaggingCommitIndexException;
import io.microraft.exception.NotLeaderException;
import io.microraft.exception.RaftException;
import io.microraft.impl.handler.AppendEntriesFailureResponseHandler;
import io.microraft.impl.handler.AppendEntriesRequestHandler;
import io.microraft.impl.handler.AppendEntriesSuccessResponseHandler;
import io.microraft.impl.handler.InstallSnapshotRequestHandler;
import io.microraft.impl.handler.InstallSnapshotResponseHandler;
import io.microraft.impl.handler.PreVoteRequestHandler;
import io.microraft.impl.handler.PreVoteResponseHandler;
import io.microraft.impl.handler.TriggerLeaderElectionHandler;
import io.microraft.impl.handler.VoteRequestHandler;
import io.microraft.impl.handler.VoteResponseHandler;
import io.microraft.impl.log.RaftLog;
import io.microraft.impl.model.DefaultRaftModelFactory;
import io.microraft.impl.report.RaftLogStatsImpl;
import io.microraft.impl.report.RaftNodeReportImpl;
import io.microraft.impl.state.FollowerState;
import io.microraft.impl.state.LeaderState;
import io.microraft.impl.state.LeadershipTransferState;
import io.microraft.impl.state.QueryState;
import io.microraft.impl.state.RaftGroupMembersState;
import io.microraft.impl.state.RaftGroupTermState;
import io.microraft.impl.state.RaftState;
import io.microraft.impl.task.LeaderElectionTimeoutTask;
import io.microraft.impl.task.MembershipChangeTask;
import io.microraft.impl.task.PreVoteTask;
import io.microraft.impl.task.PreVoteTimeoutTask;
import io.microraft.impl.task.QueryTask;
import io.microraft.impl.task.RaftNodeStatusAwareTask;
import io.microraft.impl.task.ReplicateTask;
import io.microraft.impl.util.Long2ObjectHashMap;
import io.microraft.impl.util.OrderedFuture;
import io.microraft.integration.RaftNodeRuntime;
import io.microraft.integration.StateMachine;
import io.microraft.model.RaftModelFactory;
import io.microraft.model.groupop.RaftGroupOp;
import io.microraft.model.groupop.TerminateRaftGroupOp;
import io.microraft.model.groupop.UpdateRaftGroupMembersOp;
import io.microraft.model.log.BaseLogEntry;
import io.microraft.model.log.LogEntry;
import io.microraft.model.log.SnapshotChunk;
import io.microraft.model.log.SnapshotEntry;
import io.microraft.model.message.AppendEntriesFailureResponse;
import io.microraft.model.message.AppendEntriesRequest;
import io.microraft.model.message.AppendEntriesRequest.AppendEntriesRequestBuilder;
import io.microraft.model.message.AppendEntriesSuccessResponse;
import io.microraft.model.message.InstallSnapshotRequest;
import io.microraft.model.message.InstallSnapshotResponse;
import io.microraft.model.message.PreVoteRequest;
import io.microraft.model.message.PreVoteResponse;
import io.microraft.model.message.RaftMessage;
import io.microraft.model.message.TriggerLeaderElectionRequest;
import io.microraft.model.message.VoteRequest;
import io.microraft.model.message.VoteResponse;
import io.microraft.persistence.NopRaftStore;
import io.microraft.persistence.RaftStore;
import io.microraft.persistence.RestoredRaftState;
import io.microraft.report.RaftGroupMembers;
import io.microraft.report.RaftNodeReport;
import io.microraft.report.RaftNodeReport.RaftNodeReportReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static io.microraft.RaftConfig.DEFAULT_RAFT_CONFIG;
import static io.microraft.RaftNodeStatus.ACTIVE;
import static io.microraft.RaftNodeStatus.INITIAL;
import static io.microraft.RaftNodeStatus.TERMINATED;
import static io.microraft.RaftNodeStatus.TERMINATING_RAFT_GROUP;
import static io.microraft.RaftNodeStatus.UPDATING_RAFT_GROUP_MEMBER_LIST;
import static io.microraft.RaftNodeStatus.isTerminal;
import static io.microraft.RaftRole.FOLLOWER;
import static io.microraft.RaftRole.LEADER;
import static io.microraft.impl.util.RandomPicker.getRandomInt;
import static io.microraft.model.log.SnapshotEntry.isNonInitial;
import static java.lang.Math.min;
import static java.util.Arrays.sort;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

/**
 * Implementation of {@link RaftNode}.
 * <p>
 * Each Raft node runs in a single-threaded manner with an event-based
 * approach. Raft node uses {@link RaftNodeRuntime} to run its tasks,
 * {@link StateMachine} to execute committed operations on the user-supplied
 * state machine, and {@link RaftStore} to persist internal Raft state to
 * stable storage.
 *
 * @author mdogan
 * @author metanet
 */
@SuppressWarnings({"checkstyle:methodcount", "checkstyle:classdataabstractioncoupling", "checkstyle" + ":classfanoutcomplexity"})
public class RaftNodeImpl
        implements RaftNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftNode.class);
    private static final long LEADER_ELECTION_TIMEOUT_NOISE_MILLIS = 100;
    private static final float KEPT_LOG_ENTRY_RATIO_BEFORE_SNAPSHOT_INDEX = 0.1f;

    private final Object groupId;
    private final RaftState state;
    private final RaftConfig config;
    private final RaftNodeRuntime runtime;
    private final StateMachine stateMachine;
    private final RaftModelFactory modelFactory;
    private final String localEndpointStr;
    private final Long2ObjectHashMap<OrderedFuture> futures = new Long2ObjectHashMap<>();

    private final int maxLogEntryCountToKeepAfterSnapshot;
    private final Runnable leaderBackoffResetTask;
    private final Runnable leaderFlushTask;

    private long lastLeaderHeartbeatTimestamp;
    private volatile RaftNodeStatus status = INITIAL;

    private int takeSnapshotCount;
    private int installSnapshotCount;

    @SuppressWarnings("checkstyle:executablestatementcount")
    public RaftNodeImpl(Object groupId, RaftEndpoint localEndpoint, Collection<RaftEndpoint> initialGroupMembers,
                        RaftConfig config, RaftNodeRuntime runtime, StateMachine stateMachine, RaftModelFactory modelFactory,
                        RaftStore store) {
        requireNonNull(groupId);
        requireNonNull(localEndpoint);
        requireNonNull(initialGroupMembers);
        requireNonNull(store);
        requireNonNull(config);
        requireNonNull(runtime);
        requireNonNull(stateMachine);
        requireNonNull(modelFactory);
        this.groupId = groupId;
        this.runtime = runtime;
        this.stateMachine = stateMachine;
        this.modelFactory = modelFactory;
        this.config = config;
        this.localEndpointStr = localEndpoint.getId() + "<" + groupId + ">";
        this.maxLogEntryCountToKeepAfterSnapshot = Math
                .max(1, (int) (config.getCommitCountToTakeSnapshot() * KEPT_LOG_ENTRY_RATIO_BEFORE_SNAPSHOT_INDEX));
        int logCapacity = config.getCommitCountToTakeSnapshot() + config.getMaxUncommittedLogEntryCount()
                + maxLogEntryCountToKeepAfterSnapshot;
        this.state = RaftState.create(groupId, localEndpoint, initialGroupMembers, logCapacity, store);
        this.leaderBackoffResetTask = new LeaderBackoffResetTask();
        if (store instanceof NopRaftStore) {
            this.leaderFlushTask = null;
        } else {
            this.leaderFlushTask = new LeaderFlushTask();
        }
    }

    @SuppressWarnings("checkstyle:executablestatementcount")
    public RaftNodeImpl(Object groupId, RestoredRaftState restoredState, RaftConfig config, RaftNodeRuntime runtime,
                        StateMachine stateMachine, RaftModelFactory modelFactory, RaftStore store) {
        requireNonNull(groupId);
        requireNonNull(store);
        requireNonNull(config);
        requireNonNull(runtime);
        requireNonNull(stateMachine);
        requireNonNull(modelFactory);
        this.groupId = groupId;
        this.runtime = runtime;
        this.stateMachine = stateMachine;
        this.modelFactory = modelFactory;
        this.config = config;
        this.localEndpointStr = restoredState.getLocalEndpoint().getId() + "<" + groupId + ">";
        this.maxLogEntryCountToKeepAfterSnapshot = Math
                .max(1, (int) (config.getCommitCountToTakeSnapshot() * KEPT_LOG_ENTRY_RATIO_BEFORE_SNAPSHOT_INDEX));
        int logCapacity = config.getCommitCountToTakeSnapshot() + config.getMaxUncommittedLogEntryCount()
                + maxLogEntryCountToKeepAfterSnapshot;
        this.state = RaftState.restore(groupId, restoredState, logCapacity, store);
        this.leaderBackoffResetTask = new LeaderBackoffResetTask();
        if (store instanceof NopRaftStore) {
            this.leaderFlushTask = null;
        } else {
            this.leaderFlushTask = new LeaderFlushTask();
        }
    }

    /**
     * Returns true if a new operation is allowed to be replicated. This method
     * must be invoked only when the local Raft node is the Raft group leader.
     * <p>
     * Replication is not allowed, when;
     * <ul>
     * <li>The local Raft node is terminating, terminated or left the group.</li>
     * <li>The local Raft log has no more empty slots for uncommitted entries.</li>
     * <li>The given operation is a {@link RaftGroupOp} and there's an ongoing
     * membership change in group.</li>
     * <li>The operation is a membership change and there's no committed entry
     * in the current term yet.</li>
     * </ul>
     *
     * @see RaftNodeStatus
     * @see RaftGroupOp
     * @see RaftConfig#getMaxUncommittedLogEntryCount()
     * @see StateMachine#getNewTermOperation()
     */
    public boolean canReplicateNewOperation(Object operation) {
        if (isTerminal(status)) {
            return false;
        }

        RaftLog log = state.log();
        long lastLogIndex = log.lastLogOrSnapshotIndex();
        long commitIndex = state.commitIndex();
        if (lastLogIndex - commitIndex >= config.getMaxUncommittedLogEntryCount()) {
            return false;
        }

        if (status == TERMINATING_RAFT_GROUP) {
            return false;
        } else if (status == UPDATING_RAFT_GROUP_MEMBER_LIST) {
            return state.effectiveGroupMembers().isKnownMember(getLocalEndpoint()) && !(operation instanceof RaftGroupOp);
        }

        if (operation instanceof UpdateRaftGroupMembersOp) {
            // the leader must have committed an entry in its term to make a membership change
            // https://groups.google.com/forum/#!msg/raft-dev/t4xj6dJTP6E/d2D9LrWRza8J

            // last committed entry is either in the last snapshot or still in the log
            BaseLogEntry lastCommittedEntry =
                    commitIndex == log.snapshotIndex() ? log.snapshotEntry() : log.getLogEntry(commitIndex);
            assert lastCommittedEntry != null;

            return lastCommittedEntry.getTerm() == state.term();
        }

        return state.leadershipTransferState() == null;
    }

    /**
     * Returns true if a new query is currently allowed to be executed without
     * appending an entry to the Raft log. This method must be invoked only
     * when the local Raft node is the leader.
     * <p>
     * A new linearizable query execution is not allowed when:
     * <ul>
     * <li>The local Raft node is terminating, terminated or left the group.</li>
     * <li>If the leader has not yet committed an entry in the current term.
     * See Section 6.4 of Raft Dissertation.</li>
     * <li>There are already a lot of queries waiting to be executed.</li>
     * </ul>
     *
     * @see RaftNodeStatus
     * @see RaftConfig#getMaxUncommittedLogEntryCount()
     */
    public boolean canQueryLinearizable() {
        if (isTerminal(status)) {
            return false;
        }

        long commitIndex = state.commitIndex();
        RaftLog log = state.log();

        // If the leader has not yet marked an entry from its current term committed, it waits until it has done so.
        // (§6.4)
        // last committed entry is either in the last snapshot or still in the log
        BaseLogEntry lastCommittedEntry = commitIndex == log.snapshotIndex() ? log.snapshotEntry() : log.getLogEntry(commitIndex);
        assert lastCommittedEntry != null;

        if (lastCommittedEntry.getTerm() != state.term()) {
            return false;
        }

        // We can execute multiple queries at one-shot without appending to the Raft log,
        // and we use the maxUncommittedEntryCount configuration parameter to upper-bound
        // the number of queries that are collected until the heartbeat round is done.
        QueryState queryState = state.leaderState().queryState();
        return queryState.queryCount() < config.getMaxUncommittedLogEntryCount();
    }

    private void scheduleRaftStateSummaryPublishTask() {
        schedule(new RaftStateSummaryPublishTask(), SECONDS.toMillis(config.getRaftNodeReportPublishPeriodSecs()));
    }

    private void scheduleHeartbeatTask() {
        schedule(new HeartbeatTask(), config.getLeaderHeartbeatPeriodMillis());
    }

    public void sendSnapshotChunks(RaftEndpoint follower, long snapshotIndex, List<Integer> requestedSnapshotChunkIndices) {
        // this node can be a leader or a follower!

        LeaderState leaderState = state.leaderState();
        FollowerState followerState = leaderState != null ? leaderState.getFollowerState(follower) : null;
        SnapshotEntry snapshotEntry = state.log().snapshotEntry();
        List<SnapshotChunk> snapshotChunksToSend;

        if (snapshotEntry.getIndex() == snapshotIndex) {
            if (followerState != null) {
                followerState.responseReceived();
            }

            List<SnapshotChunk> snapshotChunks = (List<SnapshotChunk>) snapshotEntry.getOperation();
            snapshotChunksToSend = requestedSnapshotChunkIndices.stream().map(snapshotChunks::get).collect(toList());
            LOGGER.debug("{} sending snapshot chunks: {} to {} for snapshot index: {}", localEndpointStr,
                    requestedSnapshotChunkIndices, follower.getId(), snapshotIndex);
        } else if (snapshotEntry.getIndex() > snapshotIndex) {
            if (leaderState == null) {
                // We allow only the Raft leader to initiate a new snapshot
                // installation process.
                LOGGER.debug("{} cannot send requested snapshot chunks: {} for snapshot index:{} to {} because the "
                                + "current snapshot index: {} and I am not the leader!", localEndpointStr, requestedSnapshotChunkIndices,
                        snapshotIndex, follower, snapshotEntry.getIndex());
                return;
            }

            snapshotChunksToSend = emptyList();
            LOGGER.debug("{} sending empty snapshot chunk list to {} because requested snapshot index: "
                            + "{} is different than the latest snapshot index: {}", localEndpointStr, follower.getId(), snapshotIndex,
                    snapshotEntry.getIndex());
        } else {
            LOGGER.error("{} requested snapshot index: {} for snapshot chunk indices: {} from {} is bigger than "
                            + "current snapshot index: {}", localEndpointStr, snapshotIndex, requestedSnapshotChunkIndices, follower,
                    snapshotEntry.getIndex());
            return;
        }

        RaftMessage request = modelFactory.createInstallSnapshotRequestBuilder().setGroupId(getGroupId())
                                          .setSender(getLocalEndpoint()).setTerm(state.term())
                                          .setSenderLeader(leaderState != null).setSnapshotTerm(snapshotEntry.getTerm())
                                          .setSnapshotIndex(snapshotEntry.getIndex())
                                          .setTotalSnapshotChunkCount(snapshotEntry.getSnapshotChunkCount())
                                          .setSnapshotChunks(snapshotChunksToSend)
                                          .setGroupMembersLogIndex(snapshotEntry.getGroupMembersLogIndex())
                                          .setGroupMembers(snapshotEntry.getGroupMembers())
                                          .setQueryRound(leaderState != null ? leaderState.queryRound() : 0).build();

        runtime.send(follower, request);

        if (followerState != null) {
            followerState.setMaxRequestBackoff();
            scheduleLeaderBackoffResetTask(leaderState);
        }
    }

    @Nonnull
    @Override
    public Object getGroupId() {
        return groupId;
    }

    @Nonnull
    @Override
    public RaftEndpoint getLocalEndpoint() {
        return state.localEndpoint();
    }

    @Nonnull
    @Override
    public RaftGroupTermState getTerm() {
        // volatile read
        return state.termState();
    }

    @Nonnull
    @Override
    public RaftNodeStatus getStatus() {
        // volatile read
        return status;
    }

    /**
     * Updates status of the Raft node with the given status.
     */
    public void setStatus(RaftNodeStatus newStatus) {
        if (isTerminal(status)) {
            throw new IllegalStateException("Cannot set status: " + newStatus + " since already " + this.status);
        }

        RaftNodeStatus prevStatus = this.status;
        this.status = newStatus;

        if (prevStatus != newStatus) {
            if (newStatus == ACTIVE) {
                LOGGER.info("{} Status is set to {}", localEndpointStr, newStatus);
            } else {
                LOGGER.warn("{} Status is set to {}", localEndpointStr, newStatus);
            }
        }

        onRaftNodeStateChange(RaftNodeReportReason.STATUS_CHANGE);
    }

    @Nonnull
    @Override
    public RaftGroupMembersState getInitialMembers() {
        return state.initialMembers();
    }

    @Nonnull
    @Override
    public RaftGroupMembersState getCommittedMembers() {
        return state.committedGroupMembers();
    }

    @Nonnull
    @Override
    public RaftGroupMembersState getEffectiveMembers() {
        return state.effectiveGroupMembers();
    }

    @Nonnull
    @Override
    public CompletableFuture<Ordered<Object>> start() {
        OrderedFuture<Object> future = new OrderedFuture<>();
        if (status != INITIAL) {
            future.fail(new IllegalStateException("Cannot start RaftNode when " + status));
            return future;
        }

        runtime.execute(() -> {
            if (status != INITIAL) {
                future.fail(new IllegalStateException("Cannot start RaftNode of `" + localEndpointStr + " when " + status));
                return;
            }

            LOGGER.info("{} Starting for {} with {} members: {}", localEndpointStr, groupId, state.memberCount(),
                    state.members());

            Throwable failure = null;
            try {
                initRestoredState();
                state.init();

                runtime.execute(new PreVoteTask(RaftNodeImpl.this, 0));
                scheduleHeartbeatTask();
                scheduleRaftStateSummaryPublishTask();

                // status could be UPDATING_GROUP_MEMBER_LIST after restoring Raft state
                // so we only switch to ACTIVE only if status is INITIAL
                if (status == INITIAL) {
                    setStatus(ACTIVE);
                }

                LOGGER.info("{} started.", localEndpointStr);
            } catch (Throwable t) {
                failure = t;
                LOGGER.error(localEndpointStr + " could not start", t);

                setStatus(TERMINATED);
            } finally {
                if (failure == null) {
                    future.completeNull(state.commitIndex());
                } else {
                    future.fail(failure);
                }
            }
        });

        return future;
    }

    @Nonnull
    @Override
    public CompletableFuture terminate() {
        if (isTerminal(status)) {
            return CompletableFuture.completedFuture(null);
        }

        OrderedFuture<Object> future = new OrderedFuture<>();

        runtime.execute(() -> {
            if (isTerminal(status)) {
                future.completeNull(state.commitIndex());
                return;
            }

            Throwable failure = null;
            try {
                if (status != INITIAL) {
                    invalidateFuturesFrom(state.commitIndex() + 1);

                    LeaderState leaderState = state.leaderState();
                    if (leaderState != null) {
                        leaderState.queryState().fail(newNotLeaderException());
                    }

                    setStatus(TERMINATED);
                    state.completeLeadershipTransfer(newNotLeaderException());
                } else {
                    setStatus(TERMINATED);
                }
            } catch (Throwable t) {
                failure = t;
                LOGGER.error("Failure during termination of " + localEndpointStr, t);
                if (status != TERMINATED) {
                    setStatus(TERMINATED);
                }
            } finally {
                if (failure == null) {
                    future.completeNull(state.commitIndex());
                } else {
                    future.fail(failure);
                }
            }
        });

        return future;
    }

    @Override
    public void handle(@Nonnull RaftMessage message) {
        if (isTerminal(status)) {
            LOGGER.warn("{} will not handle {} because {}", localEndpointStr, message, status);
            return;
        }

        Runnable handler;
        if (message instanceof AppendEntriesRequest) {
            handler = new AppendEntriesRequestHandler(this, (AppendEntriesRequest) message);
        } else if (message instanceof AppendEntriesSuccessResponse) {
            handler = new AppendEntriesSuccessResponseHandler(this, (AppendEntriesSuccessResponse) message);
        } else if (message instanceof AppendEntriesFailureResponse) {
            handler = new AppendEntriesFailureResponseHandler(this, (AppendEntriesFailureResponse) message);
        } else if (message instanceof InstallSnapshotRequest) {
            handler = new InstallSnapshotRequestHandler(this, (InstallSnapshotRequest) message);
        } else if (message instanceof InstallSnapshotResponse) {
            handler = new InstallSnapshotResponseHandler(this, (InstallSnapshotResponse) message);
        } else if (message instanceof VoteRequest) {
            handler = new VoteRequestHandler(this, (VoteRequest) message);
        } else if (message instanceof VoteResponse) {
            handler = new VoteResponseHandler(this, (VoteResponse) message);
        } else if (message instanceof PreVoteRequest) {
            handler = new PreVoteRequestHandler(this, (PreVoteRequest) message);
        } else if (message instanceof PreVoteResponse) {
            handler = new PreVoteResponseHandler(this, (PreVoteResponse) message);
        } else if (message instanceof TriggerLeaderElectionRequest) {
            handler = new TriggerLeaderElectionHandler(this, (TriggerLeaderElectionRequest) message);
        } else {
            throw new IllegalArgumentException("Invalid Raft msg: " + message);
        }

        runtime.execute(handler);
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<Ordered<T>> replicate(@Nullable Object operation) {
        OrderedFuture<T> future = new OrderedFuture<>();
        return executeIfRunning(new ReplicateTask(this, operation, future), future);
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<Ordered<T>> query(@Nullable Object operation, @Nonnull QueryPolicy queryPolicy,
                                                   long minCommitIndex) {
        OrderedFuture<T> future = new OrderedFuture<>();
        return executeIfRunning(new QueryTask(this, operation, queryPolicy, minCommitIndex, future), future);
    }

    @Nonnull
    @Override
    public CompletableFuture<Ordered<RaftGroupMembers>> changeMembership(@Nonnull RaftEndpoint endpoint,
                                                                         @Nonnull MembershipChangeMode mode,
                                                                         long expectedGroupMembersCommitIndex) {
        OrderedFuture<RaftGroupMembers> future = new OrderedFuture<>();
        Runnable task = new MembershipChangeTask(this, future, endpoint, mode, expectedGroupMembersCommitIndex);
        return executeIfRunning(task, future);
    }

    @Nonnull
    @Override
    public CompletableFuture<Ordered<Object>> transferLeadership(@Nonnull RaftEndpoint endpoint) {
        OrderedFuture<Object> future = new OrderedFuture<>();
        return executeIfRunning(() -> initLeadershipTransfer(endpoint, future), future);
    }

    @Nonnull
    @Override
    public CompletableFuture<Ordered<Object>> terminateGroup() {
        return replicate(modelFactory.createTerminateRaftGroupOpBuilder().build());
    }

    @Nonnull
    @Override
    public CompletableFuture<Ordered<RaftNodeReport>> getReport() {
        OrderedFuture<RaftNodeReport> future = new OrderedFuture<>();
        return executeIfRunning(() -> {
            try {
                future.complete(state.commitIndex(), newReport(RaftNodeReportReason.API_CALL));
            } catch (Throwable t) {
                future.fail(t);
            }
        }, future);
    }

    private RaftNodeReportImpl newReport(RaftNodeReportReason reason) {
        return new RaftNodeReportImpl(reason, groupId, state.localEndpoint(), state.initialMembers(),
                state.committedGroupMembers(), state.effectiveGroupMembers(), state.role(), status, state.termState(),
                newLogReport());
    }

    private RaftLogStatsImpl newLogReport() {
        return new RaftLogStatsImpl(state.commitIndex(), state.log().lastLogOrSnapshotEntry(), state.log().snapshotEntry(),
                takeSnapshotCount, installSnapshotCount);
    }

    private <T> OrderedFuture<T> executeIfRunning(Runnable task, OrderedFuture<T> future) {
        if (!isTerminal(status)) {
            runtime.execute(task);
        } else {
            future.fail(newNotLeaderException());
        }

        return future;
    }

    public RaftException newNotLeaderException() {
        return new NotLeaderException(getLocalEndpoint(), isTerminal(status) ? null : getLeaderEndpoint());
    }

    @Nullable
    public RaftEndpoint getLeaderEndpoint() {
        // volatile read
        return state.leader();
    }

    /**
     * Schedules a task to reset append entries request backoff periods,
     * if not scheduled already.
     *
     * @see RaftConfig#getLeaderBackoffDurationMillis()
     */
    private void scheduleLeaderBackoffResetTask(LeaderState leaderState) {
        if (leaderState.isRequestBackoffResetTaskScheduled()) {
            return;
        }

        leaderState.requestBackoffResetTaskScheduled(true);
        schedule(leaderBackoffResetTask, config.getLeaderBackoffDurationMillis());
    }

    private void schedule(Runnable task, long delayMillis) {
        runtime.schedule(task, delayMillis, MILLISECONDS);
    }

    /**
     * Applies the committed log entries between {@code lastApplied} and
     * {@code commitIndex}, if there's any available. If new entries are
     * applied, {@link RaftState}'s {@code lastApplied} field is also updated.
     *
     * @see RaftState#lastApplied()
     * @see RaftState#commitIndex()
     */
    public void applyLogEntries() {
        // Reject logs we've applied already
        long commitIndex = state.commitIndex();
        long lastApplied = state.lastApplied();

        if (commitIndex == lastApplied) {
            return;
        }

        // If commitIndex > lastApplied: increment lastApplied, apply log[lastApplied] to state machine (§5.3)
        assert commitIndex > lastApplied :
                "commit index: " + commitIndex + " cannot be smaller than last applied: " + lastApplied;

        // Apply all the preceding logs
        RaftLog log = state.log();
        for (long idx = state.lastApplied() + 1; idx <= commitIndex; idx++) {
            LogEntry entry = log.getLogEntry(idx);
            if (entry == null) {
                String msg = localEndpointStr + " Failed to get log entry at index: " + idx;
                LOGGER.error(msg);
                throw new AssertionError(msg);
            }

            applyLogEntry(entry);

            // Update the lastApplied index
            state.lastApplied(idx);
        }

        assert (status != TERMINATED || commitIndex == log.lastLogOrSnapshotIndex()) :
                "commit index: " + commitIndex + " must be equal to " + log.lastLogOrSnapshotIndex() + " on " + "termination.";

        // TODO [basri] why do we check the role here ???
        if (state.role() == LEADER || state.role() == FOLLOWER) {
            tryTakeSnapshot();
        }
    }

    /**
     * Applies the log entry by executing its operation and sets execution
     * result to the related future if any available.
     */
    private void applyLogEntry(LogEntry entry) {
        LOGGER.debug("{} Processing {}", localEndpointStr, entry);

        Object response = null;
        Object operation = entry.getOperation();
        if (operation instanceof RaftGroupOp) {
            if (operation instanceof TerminateRaftGroupOp) {
                setStatus(TERMINATED);
                runtime.onRaftGroupTerminated();
            } else if (operation instanceof UpdateRaftGroupMembersOp) {
                if (state.effectiveGroupMembers().getLogIndex() < entry.getIndex()) {
                    setStatus(UPDATING_RAFT_GROUP_MEMBER_LIST);
                    updateGroupMembers(entry.getIndex(), ((UpdateRaftGroupMembersOp) operation).getMembers());
                }

                assert status == UPDATING_RAFT_GROUP_MEMBER_LIST : "STATUS: " + status;
                assert state.effectiveGroupMembers().getLogIndex() == entry.getIndex();

                state.commitGroupMembers();

                UpdateRaftGroupMembersOp groupOp = (UpdateRaftGroupMembersOp) operation;
                if (groupOp.getEndpoint().equals(getLocalEndpoint()) && groupOp.getMode() == MembershipChangeMode.REMOVE) {
                    setStatus(TERMINATED);
                } else {
                    setStatus(ACTIVE);
                }
                response = state.committedGroupMembers();
            } else {
                response = new IllegalArgumentException("Invalid Raft group operation: " + operation);
            }
        } else {
            try {
                response = stateMachine.runOperation(entry.getIndex(), operation);
            } catch (Throwable t) {
                LOGGER.error(
                        localEndpointStr + " execution of " + operation + " at commit index: " + entry.getIndex() + " failed.",
                        t);
                response = t;
            }
        }

        completeFuture(entry.getIndex(), response);
    }

    /**
     * Updates the last leader heartbeat timestamp to now
     */
    public void leaderHeartbeatReceived() {
        lastLeaderHeartbeatTimestamp = Math.max(lastLeaderHeartbeatTimestamp, System.currentTimeMillis());
    }

    /**
     * Returns the Raft state
     */
    public RaftState state() {
        return state;
    }

    /**
     * Registers the given future with its {@code entryIndex}. This future will
     * be notified when the corresponding operation is committed or its log
     * entry is reverted.
     */
    public void registerFuture(long entryIndex, OrderedFuture future) {
        OrderedFuture f = futures.put(entryIndex, future);
        assert f == null : "Future object is already registered for entry index: " + entryIndex;
    }

    /**
     * Completes the denoted with the given result.
     */
    private void completeFuture(long logIndex, Object result) {
        OrderedFuture f = futures.remove(logIndex);
        if (f != null) {
            if (result instanceof Throwable) {
                f.fail((Throwable) result);
            } else {
                f.complete(logIndex, result);
            }
        }
    }

    /**
     * Completes futures with {@link NotLeaderException} for
     * indices greater than or equal to the given index.
     * Note that {@code entryIndex} is inclusive.
     */
    public void invalidateFuturesFrom(long entryIndex) {
        Throwable t = newNotLeaderException();
        int count = 0;
        Iterator<Entry<Long, OrderedFuture>> iterator = futures.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Long, OrderedFuture> entry = iterator.next();
            long index = entry.getKey();
            if (index >= entryIndex) {
                entry.getValue().fail(t);
                iterator.remove();
                count++;
            }
        }

        if (count > 0) {
            LOGGER.warn("{} Invalidated {} futures from log index: {}", localEndpointStr, count, entryIndex);
        }
    }

    /**
     * Takes a snapshot if the advance in {@code commitIndex} is equal to
     * {@link RaftConfig#getCommitCountToTakeSnapshot()}.
     * <p>
     * Snapshot is not created if there's an ongoing membership change or
     * the Raft group is being terminated.
     */
    private void tryTakeSnapshot() {
        long commitIndex = state.commitIndex();
        if ((commitIndex - state.log().snapshotIndex()) < config.getCommitCountToTakeSnapshot()) {
            return;
        }

        if (isTerminal(status)) {
            // If the status is UPDATING_MEMBER_LIST or TERMINATING, it means the status is normally ACTIVE
            // and there is an appended but not-yet committed RaftGroupOp.
            // If the status is terminal, then there will not be any new appends.
            return;
        }

        RaftLog log = state.log();
        List<Object> chunkObjects = new ArrayList<>();
        try {
            stateMachine.takeSnapshot(commitIndex, chunkObjects::add);
        } catch (Throwable t) {
            throw new RaftException(localEndpointStr + " Could not take snapshot at commit index: " + commitIndex, state.leader(),
                    t);
        }

        ++takeSnapshotCount;

        int snapshotTerm = log.getLogEntry(commitIndex).getTerm();
        RaftGroupMembersState members = state.committedGroupMembers();
        List<SnapshotChunk> snapshotChunks = new ArrayList<>();
        for (int chunkIndex = 0, chunkCount = chunkObjects.size(); chunkIndex < chunkCount; chunkIndex++) {
            SnapshotChunk snapshotChunk = modelFactory.createSnapshotChunkBuilder().setTerm(snapshotTerm).setIndex(commitIndex)
                                                      .setOperation(chunkObjects.get(chunkIndex))
                                                      .setSnapshotChunkIndex(chunkIndex).setSnapshotChunkCount(chunkCount)
                                                      .setGroupMembersLogIndex(members.getLogIndex())
                                                      .setGroupMembers(members.getMembers()).build();

            snapshotChunks.add(snapshotChunk);
        }

        for (SnapshotChunk snapshotChunk : snapshotChunks) {
            try {
                state.store().persistSnapshotChunk(snapshotChunk);
            } catch (IOException e) {
                throw new RaftException(e);
            }
        }

        SnapshotEntry snapshotEntry = modelFactory.createSnapshotEntryBuilder().setTerm(snapshotTerm).setIndex(commitIndex)
                                                  .setSnapshotChunks(snapshotChunks)
                                                  .setGroupMembersLogIndex(members.getLogIndex())
                                                  .setGroupMembers(members.getMembers()).build();

        long highestLogIndexToTruncate = commitIndex - maxLogEntryCountToKeepAfterSnapshot;
        LeaderState leaderState = state.leaderState();
        if (leaderState != null) {
            long[] matchIndices = leaderState.matchIndices();
            // Last slot is reserved for leader index and always zero.

            // If there is at least one follower with unknown match index,
            // its log can be close to the leader's log so we are keeping the old log entries.
            boolean allMatchIndicesKnown = Arrays.stream(matchIndices, 0, matchIndices.length - 1).noneMatch(i -> i == 0);

            if (allMatchIndicesKnown) {
                // Otherwise, we will keep the log entries until the minimum match index
                // that is bigger than (commitIndex - maxNumberOfLogsToKeepAfterSnapshot).
                // If there is no such follower (all of the minority followers are far behind),
                // then there is no need to keep the old log entries.
                highestLogIndexToTruncate = Arrays.stream(matchIndices)
                                                  // No need to keep any log entry if all followers are up to date
                                                  .filter(i -> i < commitIndex)
                                                  .filter(i -> i > commitIndex - maxLogEntryCountToKeepAfterSnapshot)
                                                  // We should not delete the smallest matchIndex
                                                  .map(i -> i - 1).sorted().findFirst().orElse(commitIndex);
            }
        }

        int truncatedEntryCount = log.setSnapshot(snapshotEntry, highestLogIndexToTruncate);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(localEndpointStr + " " + snapshotEntry + " is taken. " + truncatedEntryCount + " entries are "
                    + "truncated.");
        }

        onRaftNodeStateChange(RaftNodeReportReason.TAKE_SNAPSHOT);
    }

    /**
     * Installs the snapshot sent by the leader if it's not already installed.
     */
    public void installSnapshot(SnapshotEntry snapshotEntry) {
        long commitIndex = state.commitIndex();

        if (commitIndex >= snapshotEntry.getIndex()) {
            throw new IllegalArgumentException(
                    "Cannot install snapshot at index: " + snapshotEntry.getIndex() + " because the current commit index is: "
                            + commitIndex);
        }

        state.commitIndex(snapshotEntry.getIndex());
        RaftLog log = state.log();
        int truncated = log.setSnapshot(snapshotEntry);
        state.snapshotChunkCollector(null);

        if (truncated > 0) {
            LOGGER.info("{} {} entries are truncated to install snapshot at commit index: {}", localEndpointStr, truncated,
                    snapshotEntry);
        }

        log.flush();

        List<Object> chunkOperations = ((List<SnapshotChunk>) snapshotEntry.getOperation()).stream()
                                                                                           .map(SnapshotChunk::getOperation)
                                                                                           .collect(toList());
        stateMachine.installSnapshot(snapshotEntry.getIndex(), chunkOperations);

        ++installSnapshotCount;
        onRaftNodeStateChange(RaftNodeReportReason.INSTALL_SNAPSHOT);

        // If I am installing a snapshot, it means I am still present in the last member list,
        // but it is possible that the last entry I appended before the snapshot could be a membership change.
        // Because of this, I need to update my status.
        // Nevertheless, I may not be present in the restored member list, which is ok.

        setStatus(ACTIVE);
        if (state.restoreGroupMembers(snapshotEntry.getGroupMembersLogIndex(), snapshotEntry.getGroupMembers())) {
            onRaftNodeStateChange(RaftNodeReportReason.GROUP_MEMBERS_CHANGE);
        }

        state.lastApplied(snapshotEntry.getIndex());
        invalidateFuturesUntil(snapshotEntry.getIndex(), newIndeterminateStateException(state.leader()));

        LOGGER.info("{} snapshot is installed at commit index: {}", localEndpointStr, snapshotEntry.getIndex());
    }

    /**
     * Updates Raft group members.
     *
     * @see RaftState#updateGroupMembers(long, Collection)
     */
    public void updateGroupMembers(long logIndex, Collection<RaftEndpoint> members) {
        state.updateGroupMembers(logIndex, members);
        onRaftNodeStateChange(RaftNodeReportReason.GROUP_MEMBERS_CHANGE);
    }

    /**
     * Reverts the Raft group members back to the committed Raft group members.
     *
     * @see RaftState#revertGroupMembers()
     */
    public void revertGroupMembers() {
        state.revertGroupMembers();
        onRaftNodeStateChange(RaftNodeReportReason.GROUP_MEMBERS_CHANGE);
    }

    private void onRaftNodeStateChange(RaftNodeReportReason reason) {
        RaftNodeReportImpl report = newReport(reason);
        if ((reason == RaftNodeReportReason.STATUS_CHANGE || reason == RaftNodeReportReason.ROLE_CHANGE
                || reason == RaftNodeReportReason.GROUP_MEMBERS_CHANGE)) {
            Object groupId = state.groupId();
            RaftGroupMembers committedMembers = report.getCommittedMembers();
            StringBuilder sb = new StringBuilder(localEndpointStr).append(" Raft Group Members {").append("groupId: ")
                                                                  .append(groupId).append(", size: ")
                                                                  .append(committedMembers.getMembers().size()).append(", term:")
                                                                  .append(report.getTerm().getTerm()).append(", logIndex: ")
                                                                  .append(committedMembers.getLogIndex()).append("} [");

            committedMembers.getMembers().forEach(member -> {
                sb.append("\n\t").append(member.getId());
                if (getLocalEndpoint().equals(member)) {
                    sb.append(" - ").append(state.role()).append(" this (").append(status).append(")");
                } else if (member.equals(state.leader())) {
                    sb.append(" - ").append(LEADER);
                }
            });
            sb.append("\n] reason: ").append(reason).append("\n");
            LOGGER.info(sb.toString());
        }

        try {
            runtime.onRaftNodeReport(report);
        } catch (Throwable t) {
            LOGGER.error(localEndpointStr + " runtime failed on handling " + report, t);
        }
    }

    /**
     * Updates the known leader endpoint.
     *
     * @param member the discovered leader endpoint
     */
    public void leader(RaftEndpoint member) {
        state.leader(member);
        onRaftNodeStateChange(RaftNodeReportReason.ROLE_CHANGE);
    }

    /**
     * Switches this Raft node to the leader role by performing the following
     * steps:
     * <ul>
     * <li>Setting the local endpoint as the current leader,</li>
     * <li>Clearing (pre)candidate states,</li>
     * <li>Initializing the leader state for the current members,</li>
     * <li>Appending an operation to the Raft log if enabled,</li>
     * <li>Scheduling the periodic heartbeat task.</li>
     * </ul>
     */
    public void toLeader() {
        state.toLeader();
        appendNewTermEntry();
        broadcastAppendEntriesRequest();
        onRaftNodeStateChange(RaftNodeReportReason.ROLE_CHANGE);
    }

    /**
     * Broadcasts append entries requests to all group members
     * according to their nextIndex parameters.
     */
    public void broadcastAppendEntriesRequest() {
        for (RaftEndpoint follower : state.remoteMembers()) {
            sendAppendEntriesRequest(follower);
        }
    }

    /**
     * Sends an append entries request to the given follower.
     * <p>
     * Log entries between follower's known nextIndex and the latest appended
     * entry index are sent as a batch, whose size can be at most
     * {@link RaftConfig#getAppendEntriesRequestBatchSize()}.
     * <p>
     * If the given follower's nextIndex is behind the latest snapshot index,
     * then an {@link InstallSnapshotRequest} is sent.
     * <p>
     * If the leader doesn't know the given follower's matchIndex
     * (i.e., its {@code matchIndex == 0}), then an empty append entries
     * request is sent to save bandwidth until the leader discovers the real
     * matchIndex of the follower.
     */
    @SuppressWarnings({"checkstyle:npathcomplexity", "checkstyle:cyclomaticcomplexity", "checkstyle:methodlength"})
    public void sendAppendEntriesRequest(RaftEndpoint follower) {
        RaftLog log = state.log();
        LeaderState leaderState = state.leaderState();
        FollowerState followerState = leaderState.getFollowerState(follower);
        if (followerState.isRequestBackoffSet()) {
            // The follower still has not sent a response for the last append request.
            // We will send a new append request either when the follower sends a response
            // or a back-off timeout occurs.
            return;
        }

        long nextIndex = followerState.nextIndex();

        // if the first log entry to be sent is put into the snapshot, check if we still keep it in the log
        // if we still keep that log entry and its previous entry, we don't need to send a snapshot
        if (nextIndex <= log.snapshotIndex() && (!log.containsLogEntry(nextIndex) || (nextIndex > 1 && !log
                .containsLogEntry(nextIndex - 1)))) {
            // We send an empty request to notify the follower so that it could
            // trigger the actual snapshot installation process...
            SnapshotEntry snapshotEntry = log.snapshotEntry();
            RaftMessage request = modelFactory.createInstallSnapshotRequestBuilder().setGroupId(getGroupId())
                                              .setSender(getLocalEndpoint()).setTerm(state.term()).setSenderLeader(true)
                                              .setSnapshotTerm(snapshotEntry.getTerm()).setSnapshotIndex(snapshotEntry.getIndex())
                                              .setTotalSnapshotChunkCount(snapshotEntry.getSnapshotChunkCount())
                                              .setSnapshotChunks(emptyList())
                                              .setGroupMembersLogIndex(snapshotEntry.getGroupMembersLogIndex())
                                              .setGroupMembers(snapshotEntry.getGroupMembers())
                                              .setQueryRound(leaderState.queryRound()).build();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        localEndpointStr + " Sending " + request + " to " + follower.getId() + " since next index: " + nextIndex
                                + " <= snapshot index: " + log.snapshotIndex());
            }

            runtime.send(follower, request);
            followerState.setMaxRequestBackoff();
            scheduleLeaderBackoffResetTask(leaderState);
            return;
        }

        AppendEntriesRequestBuilder requestBuilder = modelFactory.createAppendEntriesRequestBuilder().setGroupId(getGroupId())
                                                                 .setSender(getLocalEndpoint()).setTerm(state.term())
                                                                 .setCommitIndex(state.commitIndex())
                                                                 .setQueryRound(leaderState.queryRound());
        int prevEntryTerm = 0;
        long prevEntryIndex = 0;
        List<LogEntry> entries;
        boolean setAppendRequestBackoff = true;

        long lastLogIndex = log.lastLogOrSnapshotIndex();
        if (nextIndex > 1) {
            prevEntryIndex = nextIndex - 1;
            BaseLogEntry prevEntry = (log.snapshotIndex() == prevEntryIndex) ? log.snapshotEntry() : log
                    .getLogEntry(prevEntryIndex);
            assert prevEntry != null : "Prev entry index: " + prevEntryIndex + ", snapshot: " + log.snapshotIndex();
            prevEntryTerm = prevEntry.getTerm();

            long matchIndex = followerState.matchIndex();
            if (matchIndex == 0) {
                // Until the leader has discovered where it and the follower's logs match,
                // the leader can send AppendEntries with no entries (like heartbeats) to save bandwidth.
                // We still need to enable append request backoff here because we do not want to bombard
                // the follower before we learn its match index
                entries = emptyList();
            } else if (nextIndex <= lastLogIndex) {
                // Then, once the matchIndex immediately precedes the nextIndex,
                // the leader should begin to send the actual entries
                long end = min(nextIndex + config.getAppendEntriesRequestBatchSize(), lastLogIndex);
                entries = log.getLogEntriesBetween(nextIndex, end);
            } else {
                // The follower has caught up with the leader. Sending an empty append request as a heartbeat...
                entries = emptyList();
                setAppendRequestBackoff = false;
            }
        } else if (nextIndex == 1 && lastLogIndex > 0) {
            // Entries will be sent to the follower for the first time...
            long end = min(nextIndex + config.getAppendEntriesRequestBatchSize(), lastLogIndex);
            entries = log.getLogEntriesBetween(nextIndex, end);
        } else {
            // There is no entry in the Raft log. Sending an empty append request as a heartbeat...
            entries = emptyList();
            setAppendRequestBackoff = false;
        }

        RaftMessage request = requestBuilder.setPreviousLogTerm(prevEntryTerm).setPreviousLogIndex(prevEntryIndex)
                                            .setLogEntries(entries).build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(localEndpointStr + " Sending " + request + " to " + follower.getId() + " with next index: " + nextIndex);
        }

        if (entries.size() > 0 && entries.get(entries.size() - 1).getIndex() > leaderState.flushedLogIndex()) {
            // If I am sending any non-flushed entry to the follower, I should
            // trigger the flush task. I hope that I will flush before
            // receiving append responses from half of the followers...
            // This is a very critical optimization because it makes the leader
            // and followers flush in parallel...
            submitLeaderFlushTask(leaderState);
        }

        if (setAppendRequestBackoff) {
            followerState.setRequestBackoff();
            scheduleLeaderBackoffResetTask(leaderState);
        }

        send(request, follower);
    }

    /**
     * Sends the given Raft message to the given Raft endpoint.
     */
    public void send(RaftMessage message, RaftEndpoint target) {
        runtime.send(target, message);
    }

    private void submitLeaderFlushTask(LeaderState leaderState) {
        if (leaderFlushTask == null || leaderState.isFlushTaskSubmitted()) {
            return;
        }

        leaderState.flushTaskSubmitted(true);
        runtime.submit(leaderFlushTask);
    }

    private void appendNewTermEntry() {
        Object operation = stateMachine.getNewTermOperation();
        if (operation != null) {
            RaftLog log = state.log();
            LogEntry entry = modelFactory.createLogEntryBuilder().setTerm(state.term()).setIndex(log.lastLogOrSnapshotIndex() + 1)
                                         .setOperation(operation).build();
            log.appendEntry(entry);
        }
    }

    /**
     * Switches this Raft node to the candidate role and starts a new
     * leader election round. Regular leader elections are sticky,
     * meaning that leader stickiness will be considered by other Raft nodes
     * when they receive vote requests. A non-sticky leader election occurs
     * when the current Raft group leader tries to transfer leadership to
     * another member.
     */
    public void toCandidate(boolean sticky) {
        state.toCandidate();
        BaseLogEntry lastLogEntry = state.log().lastLogOrSnapshotEntry();

        LOGGER.info("{} Leader election started for term: {}, last log index: {}, last log term: {}", localEndpointStr,
                state.term(), lastLogEntry.getIndex(), lastLogEntry.getTerm());

        onRaftNodeStateChange(RaftNodeReportReason.ROLE_CHANGE);

        RaftMessage request = modelFactory.createVoteRequestBuilder().setGroupId(getGroupId()).setSender(getLocalEndpoint())
                                          .setTerm(state.term()).setLastLogTerm(lastLogEntry.getTerm())
                                          .setLastLogIndex(lastLogEntry.getIndex()).setSticky(sticky).build();

        state.remoteMembers().forEach(member -> send(request, member));

        schedule(new LeaderElectionTimeoutTask(this), getLeaderElectionTimeoutMs());
    }

    /**
     * Returns a randomized leader election timeout in milliseconds
     * based on the leader election timeout configuration.
     *
     * @see RaftConfig#getLeaderElectionTimeoutMillis()
     */
    public long getLeaderElectionTimeoutMs() {
        return getRandomInt((int) config.getLeaderElectionTimeoutMillis(),
                (int) (config.getLeaderElectionTimeoutMillis() + LEADER_ELECTION_TIMEOUT_NOISE_MILLIS));
    }

    /**
     * Initiates the pre-voting step for the next term. The pre-voting
     * step is executed to check if other group members would vote for
     * this Raft node if it would start a new leader election.
     */
    public void preCandidate() {
        state.initPreCandidateState();
        int nextTerm = state.term() + 1;
        BaseLogEntry entry = state.log().lastLogOrSnapshotEntry();

        RaftMessage request = modelFactory.createPreVoteRequestBuilder().setGroupId(getGroupId()).setSender(getLocalEndpoint())
                                          .setTerm(nextTerm).setLastLogTerm(entry.getTerm()).setLastLogIndex(entry.getIndex())
                                          .build();

        LOGGER.info("{} Pre-vote started for next term: {}, last log index: {}, last log term: {}", localEndpointStr, nextTerm,
                entry.getIndex(), entry.getTerm());

        state.remoteMembers().forEach(member -> send(request, member));

        schedule(new PreVoteTimeoutTask(this, state.term()), getLeaderElectionTimeoutMs());
    }

    public RaftException newCannotReplicateException() {
        return new CannotReplicateException(isTerminal(status) ? null : getLeaderEndpoint());
    }

    private void initLeadershipTransfer(RaftEndpoint targetEndpoint, OrderedFuture<Object> resultFuture) {
        if (checkLeadershipTransfer(targetEndpoint, resultFuture)) {
            return;
        }

        if (getLocalEndpoint().equals(targetEndpoint)) {
            LOGGER.warn("{} I am already the leader... There is no leadership transfer to myself.", localEndpointStr);
            resultFuture.completeNull(state.commitIndex());
            return;
        }

        if (state.initLeadershipTransfer(targetEndpoint, resultFuture)) {
            transferLeadership();
        }
    }

    private boolean checkLeadershipTransfer(RaftEndpoint targetEndpoint, OrderedFuture<Object> resultFuture) {
        if (isTerminal(status)) {
            resultFuture.fail(newNotLeaderException());
            return true;
        }

        if (!getCommittedMembers().getMembers().contains(targetEndpoint)) {
            resultFuture.fail(new IllegalArgumentException("Cannot transfer leadership to " + targetEndpoint
                    + " because it is not in the committed group member list!"));
            return true;
        }

        if (getStatus() != ACTIVE) {
            resultFuture.fail(new IllegalStateException(
                    "Cannot transfer leadership to " + targetEndpoint + " because the status is " + getStatus()));
            return true;
        }

        if (state.leaderState() == null) {
            resultFuture.fail(newNotLeaderException());
            return true;
        }

        return false;
    }

    private void transferLeadership() {
        LeaderState leaderState = state.leaderState();

        if (leaderState == null) {
            LOGGER.debug("{} Not retrying leadership transfer since not leader...", localEndpointStr);
            return;
        }

        LeadershipTransferState leadershipTransferState = state.leadershipTransferState();
        if (leadershipTransferState == null) {
            throw new IllegalStateException("No leadership transfer state!");
        }

        if (!leadershipTransferState.retry()) {
            String msg =
                    localEndpointStr + " Leadership transfer to " + leadershipTransferState.endpoint().getId() + " timed out!";
            LOGGER.warn(msg);
            state.completeLeadershipTransfer(new TimeoutException(msg));
            return;
        }

        RaftEndpoint targetEndpoint = leadershipTransferState.endpoint();
        long delayMs = leadershipTransferState.retryDelay(getLeaderElectionTimeoutMs());

        if (state.commitIndex() < state.log().lastLogOrSnapshotIndex()) {
            LOGGER.warn("{} Waiting until all appended entries to be committed before transferring leadership to {}",
                    localEndpointStr, targetEndpoint.getId());
            schedule(this::transferLeadership, delayMs);
            return;
        }

        if (leadershipTransferState.tryCount() > 1) {
            LOGGER.debug("{} Retrying leadership transfer to {}", localEndpointStr, leadershipTransferState.endpoint().getId());
        } else {
            LOGGER.info("{} Transferring leadership to {}", localEndpointStr, leadershipTransferState.endpoint().getId());
        }

        leaderState.getFollowerState(targetEndpoint).responseReceived();
        sendAppendEntriesRequest(targetEndpoint);

        BaseLogEntry entry = state.log().lastLogOrSnapshotEntry();
        RaftMessage request = modelFactory.createTriggerLeaderElectionRequestBuilder().setGroupId(getGroupId())
                                          .setSender(getLocalEndpoint()).setTerm(state.term()).setLastLogTerm(entry.getTerm())
                                          .setLastLogIndex(entry.getIndex()).build();
        send(request, targetEndpoint);

        schedule(this::transferLeadership, delayMs);
    }

    private long findQuorumMatchIndex() {
        LeaderState leaderState = state.leaderState();
        long[] indices = leaderState.matchIndices();

        // if the leader is leaving, it should not count its vote for quorum...
        if (state.isKnownMember(getLocalEndpoint())) {
            // Raft dissertation Section 10.2.1:
            // The leader may even commit an entry before it has been written to its own disk,
            // if a majority of followers have written it to their disks; this is still safe.
            long leaderIndex = leaderFlushTask == null ? state.log().lastLogOrSnapshotIndex() : leaderState.flushedLogIndex();
            indices[indices.length - 1] = leaderIndex;
        } else {
            // Remove the last empty slot reserved for leader index
            indices = Arrays.copyOf(indices, indices.length - 1);
        }

        sort(indices);

        long quorumMatchIndex = indices[(indices.length - 1) / 2];
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    localEndpointStr + " Quorum match index: " + quorumMatchIndex + ", indices: " + Arrays.toString(indices));
        }

        return quorumMatchIndex;
    }

    public boolean tryAdvanceCommitIndex() {
        // If there exists an N such that N > commitIndex, a majority of matchIndex[i] ≥ N, and log[N].term ==
        // currentTerm:
        // set commitIndex = N (§5.3, §5.4)
        long quorumMatchIndex = findQuorumMatchIndex();
        long commitIndex = state.commitIndex();
        RaftLog log = state.log();
        for (; quorumMatchIndex > commitIndex; quorumMatchIndex--) {
            // Only log entries from the leader’s current term are committed by counting replicas; once an entry
            // from the current term has been committed in this way, then all prior entries are committed indirectly
            // because of the Log Matching Property.
            LogEntry entry = log.getLogEntry(quorumMatchIndex);
            if (entry.getTerm() == state.term()) {
                commitEntries(quorumMatchIndex);
                return true;
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        localEndpointStr + " cannot commit " + entry + " since an entry from the current term: " + state.term()
                                + " is needed.");
            }
        }
        return false;
    }

    private void commitEntries(long commitIndex) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(localEndpointStr + " Setting commit index: " + commitIndex);
        }

        state.commitIndex(commitIndex);

        if (status == ACTIVE) {
            applyLogEntries();
            tryRunQueries();
        } else {
            tryRunQueries();
            applyLogEntries();
        }

        broadcastAppendEntriesRequest();
    }

    public void tryAckQueryRound(long queryRound, RaftEndpoint sender) {
        LeaderState leaderState = state.leaderState();
        if (leaderState == null) {
            return;
        }

        QueryState queryState = leaderState.queryState();
        if (queryState.tryAck(queryRound, sender)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(localEndpointStr() + " ack from " + sender.getId() + " for query round: " + queryRound);
            }

            tryRunQueries();
        }
    }

    /**
     * Returns a short string that represents identity
     * of the local Raft endpoint.
     */
    public String localEndpointStr() {
        return localEndpointStr;
    }

    public void tryRunQueries() {
        QueryState queryState = state.leaderState().queryState();
        if (queryState.queryCount() == 0) {
            return;
        }

        long commitIndex = state.commitIndex();
        if (!queryState.isMajorityAckReceived(commitIndex, state.majority())) {
            return;
        }

        Collection<Entry<Object, OrderedFuture>> operations = queryState.queries();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(localEndpointStr + " running " + operations.size() + " queries at commit index: " + commitIndex
                    + ", query round: " + queryState.queryRound());
        }

        for (Entry<Object, OrderedFuture> t : operations) {
            runQuery(t.getKey(), state.commitIndex(), t.getValue());
        }

        queryState.reset();
    }

    /**
     * Executes the given query operation sets execution result to the future.
     * <p>
     * Please note that the given operation must not make any mutation on the
     * state machine.
     */
    public void runQuery(Object operation, long minCommitIndex, OrderedFuture future) {
        try {
            long commitIndex = state.commitIndex();
            if (commitIndex >= minCommitIndex) {
                Object result = stateMachine.runOperation(commitIndex, operation);
                future.complete(commitIndex, result);
            } else {
                future.fail(new LaggingCommitIndexException(state.commitIndex(), minCommitIndex, state.leader()));
            }
        } catch (Throwable t) {
            future.fail(t);
        }
    }

    public boolean isLeaderHeartbeatTimeoutElapsed() {
        return isLeaderHeartbeatTimeoutElapsed(lastLeaderHeartbeatTimestamp);
    }

    private boolean isLeaderHeartbeatTimeoutElapsed(long timestamp) {
        long deadline;
        if (Long.MAX_VALUE - config.getLeaderHeartbeatTimeoutMillis() >= timestamp) {
            deadline = timestamp + config.getLeaderHeartbeatTimeoutMillis();
        } else {
            deadline = Long.MAX_VALUE;
        }

        return deadline < System.currentTimeMillis();
    }

    private void initRestoredState() {
        SnapshotEntry snapshotEntry = state.log().snapshotEntry();
        if (isNonInitial(snapshotEntry)) {
            List<Object> chunkOperations = ((List<SnapshotChunk>) snapshotEntry.getOperation()).stream()
                                                                                               .map(SnapshotChunk::getOperation)
                                                                                               .collect(toList());
            stateMachine.installSnapshot(snapshotEntry.getIndex(), chunkOperations);
            onRaftNodeStateChange(RaftNodeReportReason.INSTALL_SNAPSHOT);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.info(localEndpointStr + " restored " + snapshotEntry);
            } else {
                LOGGER.info(localEndpointStr + "restored snapshot commitIndex=" + snapshotEntry.getIndex());
            }
        }

        applyRestoredRaftGroupOps(snapshotEntry);
    }

    private void applyRestoredRaftGroupOps(SnapshotEntry snapshot) {
        // If there is a single Raft group operation after the last snapshot,
        // here we cannot know if the that operation is committed or not so we
        // just "prepare" the operation without committing it.
        // If there are multiple Raft group operations, it is definitely known
        // that all the operations up to the last Raft group operation are
        // committed, but the last Raft group operation may not be committed.
        // This conclusion boils down to the fact that once you append a Raft
        // group operation, you cannot append a new one before committing
        // the former.

        RaftLog log = state.log();
        LogEntry committedEntry = null;
        LogEntry lastAppliedEntry = null;

        for (long i = snapshot != null ? snapshot.getIndex() + 1 : 1; i <= log.lastLogOrSnapshotIndex(); i++) {
            LogEntry entry = log.getLogEntry(i);
            assert entry != null : "index: " + i;
            if (entry.getOperation() instanceof RaftGroupOp) {
                committedEntry = lastAppliedEntry;
                lastAppliedEntry = entry;
            }
        }

        if (committedEntry != null) {
            state.commitIndex(committedEntry.getIndex());
            applyLogEntries();
        }

        if (lastAppliedEntry != null) {
            if (lastAppliedEntry.getOperation() instanceof UpdateRaftGroupMembersOp) {
                setStatus(UPDATING_RAFT_GROUP_MEMBER_LIST);
                Collection<RaftEndpoint> members = ((UpdateRaftGroupMembersOp) lastAppliedEntry.getOperation()).getMembers();
                updateGroupMembers(lastAppliedEntry.getIndex(), members);
            } else if (lastAppliedEntry.getOperation() instanceof TerminateRaftGroupOp) {
                setStatus(TERMINATING_RAFT_GROUP);
            } else {
                throw new IllegalStateException("Invalid group command for restore: " + lastAppliedEntry);
            }
        }
    }

    public RaftModelFactory getModelFactory() {
        return modelFactory;
    }

    public boolean demoteToFollowerIfMajorityHeartbeatTimeoutElapsed() {
        LeaderState leaderState = state.leaderState();
        if (leaderState == null) {
            return true;
        }

        long majorityTimestamp = leaderState.majorityAppendEntriesResponseTimestamp(state.majority());
        if (isLeaderHeartbeatTimeoutElapsed(majorityTimestamp)) {
            LOGGER.warn("{} Demoting to {} since not received append entries responses from majority recently.", localEndpointStr,
                    FOLLOWER);
            toFollower(state.term());
            // TODO [basri] is this really necessary
            invalidateFuturesUntil(state.log().lastLogOrSnapshotIndex(), newIndeterminateStateException(null));
            return true;
        }

        return false;
    }

    /**
     * Completes futures with the given exception for indices smaller than
     * or equal to the given index. Note that {@code entryIndex} is inclusive.
     */
    private void invalidateFuturesUntil(long entryIndex, RaftException ex) {
        int count = 0;
        Iterator<Entry<Long, OrderedFuture>> iterator = futures.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Long, OrderedFuture> entry = iterator.next();
            long index = entry.getKey();
            if (index <= entryIndex) {
                entry.getValue().fail(ex);
                iterator.remove();
                count++;
            }
        }

        if (count > 0) {
            LOGGER.warn("{} Completed {} futures until log index: {} with {}", localEndpointStr, count, entryIndex, ex);
        }
    }

    /**
     * Switches this node to the follower role by clearing the known leader
     * endpoint and (pre) candidate states, and updating the term. If this Raft
     * node was leader before switching to the follower state, it may have some
     * queries waiting to be executed. Those queries are also failed with
     * {@link NotLeaderException}.
     *
     * @param term the new term to switch
     */
    public void toFollower(int term) {
        LeaderState leaderState = state.leaderState();
        state.toFollower(term);
        if (leaderState != null) {
            // this is done after toFollower() to reset the leader field
            leaderState.queryState().fail(newNotLeaderException());
        }
        onRaftNodeStateChange(RaftNodeReportReason.ROLE_CHANGE);
    }

    private RaftException newIndeterminateStateException(RaftEndpoint leader) {
        return new IndeterminateStateException(leader);
    }

    /**
     * Scheduled only on the Raft group leader with
     * {@link RaftConfig#getLeaderHeartbeatPeriodMillis()} delay to denote leader
     * liveliness by sending periodic append entries requests to followers.
     */

    public static class RaftNodeBuilderImpl
            implements RaftNodeBuilder {

        private Object groupId;
        private RaftEndpoint localMember;
        private Collection<RaftEndpoint> initialGroupMembers;
        private RestoredRaftState restoredState;
        private RaftConfig config = DEFAULT_RAFT_CONFIG;
        private RaftNodeRuntime runtime;
        private StateMachine stateMachine;
        private RaftStore store = NopRaftStore.INSTANCE;
        private RaftModelFactory modelFactory = DefaultRaftModelFactory.INSTANCE;
        private boolean done;

        @Nonnull
        @Override
        public RaftNodeBuilder setGroupId(@Nonnull Object groupId) {
            this.groupId = groupId;
            return this;
        }

        @Nonnull
        @Override
        public RaftNodeBuilder setLocalEndpoint(@Nonnull RaftEndpoint localEndpoint) {
            requireNonNull(localEndpoint);
            if (this.restoredState != null) {
                throw new IllegalStateException("Local member cannot be set when restored Raft state is provided!");
            }

            this.localMember = localEndpoint;
            return this;
        }

        @Nonnull
        @Override
        public RaftNodeBuilder setInitialGroupMembers(@Nonnull Collection<RaftEndpoint> initialGroupMembers) {
            requireNonNull(initialGroupMembers);

            if (this.restoredState != null) {
                throw new IllegalStateException("Initial group members cannot be set when restored Raft state is " + "provided!");
            }

            this.initialGroupMembers = initialGroupMembers;
            return this;
        }

        @Nonnull
        @Override
        public RaftNodeBuilder setRestoredState(@Nonnull RestoredRaftState restoredState) {
            requireNonNull(restoredState);

            if (this.localMember != null || this.initialGroupMembers != null) {
                throw new IllegalStateException(
                        "Restored state cannot be set when either local member or initial " + "group members is provided!");
            }

            this.restoredState = restoredState;
            return this;
        }

        @Nonnull
        @Override
        public RaftNodeBuilder setConfig(@Nonnull RaftConfig config) {
            requireNonNull(config);
            this.config = config;
            return this;
        }

        @Nonnull
        @Override
        public RaftNodeBuilder setRuntime(@Nonnull RaftNodeRuntime runtime) {
            requireNonNull(runtime);
            this.runtime = runtime;
            return this;
        }

        @Nonnull
        @Override
        public RaftNodeBuilder setStateMachine(@Nonnull StateMachine stateMachine) {
            requireNonNull(stateMachine);
            this.stateMachine = stateMachine;
            return this;
        }

        @Nonnull
        @Override
        public RaftNodeBuilder setStore(@Nonnull RaftStore store) {
            requireNonNull(store);
            this.store = store;
            return this;
        }

        @Nonnull
        @Override
        public RaftNodeBuilder setModelFactory(@Nonnull RaftModelFactory modelFactory) {
            requireNonNull(modelFactory);
            this.modelFactory = modelFactory;
            return this;
        }

        @Nonnull
        @Override
        public RaftNode build() {
            if (done) {
                throw new IllegalStateException("Raft node is already built!");
            }

            done = true;
            if (localMember != null && initialGroupMembers != null) {
                return new RaftNodeImpl(groupId, localMember, initialGroupMembers, config, runtime, stateMachine, modelFactory,
                        store);
            } else if (restoredState != null) {
                return new RaftNodeImpl(groupId, restoredState, config, runtime, stateMachine, modelFactory, store);
            }

            throw new IllegalStateException("RaftNodeBuilder state is not correctly initialized!");
        }

    }

    /**
     * Checks whether currently there is a known leader endpoint and triggers
     * the pre-voting mechanism there is no known leader or the leader has
     * timed out.
     */
    private class HeartbeatTask
            extends RaftNodeStatusAwareTask {
        HeartbeatTask() {
            super(RaftNodeImpl.this);
        }

        @Override
        protected void doRun() {
            try {
                if (state.leaderState() != null) {
                    if (!demoteToFollowerIfMajorityHeartbeatTimeoutElapsed()) {
                        broadcastAppendEntriesRequest();
                    }

                    return;
                }

                RaftEndpoint leader = state.leader();
                if (leader == null) {
                    if (state.role() == FOLLOWER && state.preCandidateState() == null) {
                        LOGGER.warn("{} We are FOLLOWER and there is no current leader. Will start new election round",
                                localEndpointStr);
                        triggerPreVote();
                    }
                } else if (isLeaderHeartbeatTimeoutElapsed() && state.preCandidateState() == null) {
                    LOGGER.warn("{} Current leader {}'s heartbeats are timed-out. Will start new election round.",
                            localEndpointStr, leader.getId());
                    resetLeaderAndTriggerPreVote();
                } else if (!state.committedGroupMembers().isKnownMember(leader) && state.preCandidateState() == null) {
                    LOGGER.warn("{} Current leader {} is not member anymore. Will start new election round.", localEndpointStr,
                            leader.getId());
                    resetLeaderAndTriggerPreVote();
                }
            } finally {
                scheduleHeartbeatTask();
            }
        }

        void resetLeaderAndTriggerPreVote() {
            leader(null);
            triggerPreVote();
        }

        void triggerPreVote() {
            new PreVoteTask(RaftNodeImpl.this, state.term()).run();
        }

    }

    /**
     * If the append entries request backoff period is active for any follower,
     * this task will send a new append entries request on the backoff
     * completion.
     */
    private class LeaderBackoffResetTask
            extends RaftNodeStatusAwareTask {

        LeaderBackoffResetTask() {
            super(RaftNodeImpl.this);
        }

        @Override
        protected void doRun() {
            LeaderState leaderState = state.leaderState();
            if (leaderState == null) {
                return;
            }

            leaderState.requestBackoffResetTaskScheduled(false);

            for (Entry<RaftEndpoint, FollowerState> entry : leaderState.getFollowerStates().entrySet()) {
                FollowerState followerState = entry.getValue();
                if (!followerState.isRequestBackoffSet()) {
                    continue;
                }

                if (followerState.completeBackoffRound()) {
                    // This follower has not sent a response to the last append request.
                    // Send another append request
                    sendAppendEntriesRequest(entry.getKey());
                }

                // Schedule the task again, we still have backoff flag set followers
                scheduleLeaderBackoffResetTask(leaderState);
            }
        }

    }

    private class RaftStateSummaryPublishTask
            extends RaftNodeStatusAwareTask {
        RaftStateSummaryPublishTask() {
            super(RaftNodeImpl.this);
        }

        @Override
        protected void doRun() {
            try {
                onRaftNodeStateChange(RaftNodeReportReason.PERIODIC);
            } finally {
                scheduleRaftStateSummaryPublishTask();
            }
        }

    }

    private class LeaderFlushTask
            extends RaftNodeStatusAwareTask {
        LeaderFlushTask() {
            super(RaftNodeImpl.this);
        }

        @Override
        protected void doRun() {
            LeaderState leaderState = state.leaderState();
            if (leaderState == null) {
                return;
            }

            RaftLog log = state.log();
            log.flush();

            leaderState.flushTaskSubmitted(false);
            leaderState.flushedLogIndex(log.lastLogOrSnapshotIndex());

            tryAdvanceCommitIndex();
        }

    }

}