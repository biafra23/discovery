/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.message.V5Message;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.packet.OrdinaryMessagePacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.ethereum.beacon.discovery.schema.NodeRecordFactory;
import org.ethereum.beacon.discovery.schema.NodeSession;
import org.ethereum.beacon.discovery.type.Bytes16;
import org.ethereum.beacon.discovery.util.DecryptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles {@link MessagePacket} in {@link Field#PACKET_MESSAGE} field */
public class MessagePacketHandler implements EnvelopeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(MessagePacketHandler.class);
  private final NodeRecordFactory nodeRecordFactory;

  public MessagePacketHandler(NodeRecordFactory nodeRecordFactory) {
    this.nodeRecordFactory = nodeRecordFactory;
  }

  @Override
  public void handle(Envelope envelope) {
    if (!HandlerUtil.requireField(Field.PACKET_MESSAGE, envelope)) {
      return;
    }
    if (!HandlerUtil.requireField(Field.MASKING_IV, envelope)) {
      return;
    }
    if (!HandlerUtil.requireField(Field.SESSION, envelope)) {
      return;
    }
    LOG.trace(
        "Envelope {} in MessagePacketHandler, requirements are satisfied!", envelope.getIdString());

    MessagePacket<?> packet = envelope.get(Field.PACKET_MESSAGE);
    NodeSession session = envelope.get(Field.SESSION);

    try {
      Bytes16 maskingIV = envelope.get(Field.MASKING_IV);
      V5Message message =
          packet.decryptMessage(maskingIV, session.getRecipientKey(), nodeRecordFactory);
      envelope.put(Field.MESSAGE, message);
      envelope.remove(Field.PACKET_MESSAGE);
    } catch (DecryptException e) {
      LOG.trace(
          "Failed to decrypt message [{}] from node {} in status {}. Will be sending WHOAREYOU...",
          packet,
          session.getNodeRecord(),
          session.getState());

      envelope.remove(Field.PACKET_MESSAGE);
      if (packet instanceof OrdinaryMessagePacket) {
        envelope.put(Field.UNAUTHORIZED_PACKET_MESSAGE, (OrdinaryMessagePacket) packet);
      }
    } catch (Exception ex) {
      String error =
          String.format(
              "Failed to read message [%s] from node %s in status %s",
              packet, session.getNodeRecord(), session.getState());
      LOG.debug(error, ex);
      envelope.remove(Field.PACKET_MESSAGE);
      envelope.put(Field.BAD_PACKET, packet);
    } catch (Throwable t) {
      LOG.warn(
          "Unexpected error while reading message [{}] from node {}",
          packet,
          session.getNodeRecord(),
          t);
      envelope.remove(Field.PACKET_MESSAGE);
      envelope.put(Field.BAD_PACKET, packet);
    }
  }
}
