/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.TalkHandler;
import org.ethereum.beacon.discovery.message.V5Message;
import org.ethereum.beacon.discovery.message.handler.EnrUpdateTracker.EnrUpdater;
import org.ethereum.beacon.discovery.message.handler.ExternalAddressSelector;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.ethereum.beacon.discovery.processor.DiscoveryV5MessageProcessor;
import org.ethereum.beacon.discovery.processor.MessageProcessor;
import org.ethereum.beacon.discovery.schema.NodeSession;
import org.ethereum.beacon.discovery.storage.LocalNodeRecordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandler implements EnvelopeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(MessageHandler.class);
  private final MessageProcessor messageProcessor;

  public MessageHandler(
      LocalNodeRecordStore localNodeRecordStore,
      TalkHandler talkHandler,
      EnrUpdater enrUpdater,
      ExternalAddressSelector externalAddressSelector) {

    this.messageProcessor =
        new MessageProcessor(
            new DiscoveryV5MessageProcessor(
                localNodeRecordStore, talkHandler, enrUpdater, externalAddressSelector));
  }

  @Override
  public void handle(Envelope envelope) {
    if (!HandlerUtil.requireField(Field.MESSAGE, envelope)) {
      return;
    }
    if (!HandlerUtil.requireSessionWithNodeRecord(envelope)) {
      return;
    }
    LOG.trace(
        String.format(
            "Envelope %s in MessageHandler, requirements are satisfied!", envelope.getIdString()));

    NodeSession session = envelope.get(Field.SESSION);
    V5Message message = envelope.get(Field.MESSAGE);
    try {
      messageProcessor.handleIncoming(message, session);
    } catch (Exception ex) {
      LOG.trace(
          String.format(
              "Failed to handle message %s in envelope #%s", message, envelope.getIdString()),
          ex);
      envelope.put(Field.BAD_EXCEPTION, ex);
      envelope.remove(Field.MESSAGE);
    }
  }
}
