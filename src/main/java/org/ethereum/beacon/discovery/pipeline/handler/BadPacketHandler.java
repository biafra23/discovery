/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles packet from {@link Field#BAD_PACKET}. Currently just logs it. */
public class BadPacketHandler implements EnvelopeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(BadPacketHandler.class);

  @Override
  public void handle(Envelope envelope) {
    if (!HandlerUtil.requireField(Field.BAD_PACKET, envelope)) {
      return;
    }
    LOG.trace(
        "Envelope {} in BadPacketHandler, requirements are satisfied!", envelope.getIdString());

    LOG.debug(
        "Bad packet: {} in envelope #{}: {}",
        envelope.get(Field.BAD_PACKET),
        envelope.getIdString(),
        envelope.get(Field.BAD_EXCEPTION).toString());
    // TODO: Reputation penalty etc
  }
}
