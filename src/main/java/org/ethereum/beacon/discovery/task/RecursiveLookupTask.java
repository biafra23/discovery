/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.task;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.schema.NodeRecord;
import org.ethereum.beacon.discovery.storage.KBuckets;
import org.ethereum.beacon.discovery.util.Functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecursiveLookupTask {
  private static final Logger LOG = LoggerFactory.getLogger(RecursiveLookupTask.class);
  private static final int MAX_CONCURRENT_QUERIES = 3;
  private final KBuckets buckets;
  private final FindNodesAction sendFindNodesRequest;
  private final Bytes targetNodeId;
  private final Set<Bytes> queriedNodeIds = new HashSet<>();
  private final Comparator<NodeRecord> distanceComparator;
  private int availableQuerySlots = MAX_CONCURRENT_QUERIES;
  private int remainingTotalQueryLimit;
  private final CompletableFuture<Collection<NodeRecord>> future = new CompletableFuture<>();
  private final NavigableSet<NodeRecord> foundNodes;

  public RecursiveLookupTask(
      final KBuckets buckets,
      final FindNodesAction sendFindNodesRequest,
      final int totalQueryLimit,
      final Bytes targetNodeId,
      final Bytes homeNodeId) {
    checkArgument(totalQueryLimit > 0, "Must allow positive number of queries");
    this.buckets = buckets;
    this.sendFindNodesRequest = sendFindNodesRequest;
    this.remainingTotalQueryLimit = totalQueryLimit;
    this.targetNodeId = targetNodeId;
    this.distanceComparator =
        Comparator.<NodeRecord, BigInteger>comparing(
                node -> Functions.distance(targetNodeId, node.getNodeId()))
            .reversed()
            .thenComparing(NodeRecord::getNodeId);
    this.foundNodes = new TreeSet<>(distanceComparator);
    // Don't query ourselves
    this.queriedNodeIds.add(homeNodeId);
  }

  public CompletableFuture<Collection<NodeRecord>> execute() {
    sendRequests();
    return future;
  }

  private synchronized void sendRequests() {
    checkArgument(availableQuerySlots >= 0, "Available query slots should never be negative");
    checkArgument(
        remainingTotalQueryLimit >= 0, "Remaining total query limit should never be negative");
    if (availableQuerySlots == 0 || future.isDone()) {
      return;
    }
    if (buckets.containsNode(targetNodeId)) {
      future.complete(foundNodes);
      return;
    }
    final int maxNodesToQuery = Math.min(availableQuerySlots, remainingTotalQueryLimit);

    final Stream<NodeRecord> closestNodesFromBuckets =
        buckets
            .streamClosestNodes(targetNodeId)
            .filter(record -> !queriedNodeIds.contains(record.getNodeId()))
            .limit(maxNodesToQuery);

    final Stream<NodeRecord> foundNodesToQuery =
        foundNodes.stream()
            .filter(record -> !queriedNodeIds.contains(record.getNodeId()))
            .limit(maxNodesToQuery);

    // Mix the two sources together and select the closest from either.
    queryPeers(
        Stream.concat(closestNodesFromBuckets, foundNodesToQuery)
            .sorted(distanceComparator)
            .limit(maxNodesToQuery)
            .collect(Collectors.toList()));
    if (availableQuerySlots == MAX_CONCURRENT_QUERIES) {
      // There are no in-progress queries even after we looked for more to send so must have run out
      // of possible nodes to query or reached the query limit.
      future.complete(foundNodes);
    }
  }

  private void queryPeers(final List<NodeRecord> nodesToQuery) {
    // Update state to indicate all nodes queried before we start sending requests.
    // Otherwise if any request completes synchronously we recurse back into this method and wind up
    // sending too many requests
    nodesToQuery.stream().map(NodeRecord::getNodeId).forEach(queriedNodeIds::add);
    availableQuerySlots -= nodesToQuery.size();
    remainingTotalQueryLimit -= nodesToQuery.size();
    nodesToQuery.forEach(this::queryPeer);
  }

  private void queryPeer(final NodeRecord peer) {
    sendFindNodesRequest
        .findNodes(peer, Functions.logDistance(peer.getNodeId(), targetNodeId))
        .whenComplete(
            (nodes, error) -> {
              synchronized (this) {
                availableQuerySlots++;
                if (error != null) {
                  LOG.debug("Failed to query node {}: {}", peer.getNodeId(), error.toString());
                } else {
                  foundNodes.addAll(nodes);
                }
                sendRequests();
              }
            });
  }

  public interface FindNodesAction {
    CompletableFuture<Collection<NodeRecord>> findNodes(NodeRecord sendTo, int targetDistance);
  }
}
