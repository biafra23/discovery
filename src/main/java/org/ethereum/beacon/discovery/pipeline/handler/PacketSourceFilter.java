/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.pipeline.handler;

import java.net.InetSocketAddress;
import org.ethereum.beacon.discovery.AddressAccessPolicy;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketSourceFilter implements EnvelopeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(PacketSourceFilter.class);

  private final AddressAccessPolicy addressAccessPolicy;

  public PacketSourceFilter(final AddressAccessPolicy addressAccessPolicy) {
    this.addressAccessPolicy = addressAccessPolicy;
  }

  @Override
  public void handle(final Envelope envelope) {
    if (!HandlerUtil.requireField(Field.REMOTE_SENDER, envelope)) {
      return;
    }
    final InetSocketAddress sender = envelope.get(Field.REMOTE_SENDER);
    if (!addressAccessPolicy.allow(sender)) {
      envelope.remove(Field.INCOMING);
      LOG.debug("Ignoring message from disallowed source {}", sender);
    }
  }
}
