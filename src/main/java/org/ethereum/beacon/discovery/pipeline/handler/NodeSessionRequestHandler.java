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

/**
 * Searches for node in {@link Field#NODE} and requests session resolving using {@link
 * Field#SESSION_LOOKUP}
 */
public class NodeSessionRequestHandler implements EnvelopeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(NodeSessionRequestHandler.class);

  @Override
  public void handle(Envelope envelope) {
    if (!HandlerUtil.requireField(Field.NODE, envelope)) {
      return;
    }
    LOG.trace(
        "Envelope {} in NodeSessionRequestHandler, requirements are satisfied!",
        envelope.getIdString());

    envelope.put(Field.SESSION_LOOKUP, new SessionLookup(envelope.get(Field.NODE)));
  }
}
