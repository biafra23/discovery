/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.packet.Packet;
import org.ethereum.beacon.discovery.packet.RawPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.ethereum.beacon.discovery.type.Bytes16;
import org.ethereum.beacon.discovery.util.DecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles raw BytesValue incoming data in {@link Field#INCOMING} */
public class IncomingDataPacker implements EnvelopeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(IncomingDataPacker.class);
  public static final int MAX_PACKET_SIZE = 1280;
  public static final int MIN_PACKET_SIZE = 63;
  private final Bytes16 homeNodeId;

  public IncomingDataPacker(Bytes homeNodeId) {
    this.homeNodeId = Bytes16.wrap(homeNodeId, 0);
  }

  @Override
  public void handle(Envelope envelope) {
    if (!HandlerUtil.requireField(Field.INCOMING, envelope)) {
      return;
    }
    LOG.trace(
        "Envelope {} in IncomingDataPacker, requirements are satisfied!", envelope.getIdString());

    Bytes rawPacketBytes = (Bytes) envelope.get(Field.INCOMING);
    try {
      if (rawPacketBytes.size() > MAX_PACKET_SIZE) {
        throw new DecodeException("Packet is too large: " + rawPacketBytes.size());
      }
      if (rawPacketBytes.size() < MIN_PACKET_SIZE) {
        throw new DecodeException("Packet is too small: " + rawPacketBytes.size());
      }
      RawPacket rawPacket = RawPacket.decode(rawPacketBytes);
      rawPacket.validate();
      Packet<?> packet = rawPacket.demaskPacket(homeNodeId);
      // check that AES/CTR decoded correctly

      envelope.put(Field.PACKET, packet);
      envelope.put(Field.MASKING_IV, rawPacket.getMaskingIV());
      LOG.trace("Incoming packet {} in envelope #{}", packet, envelope.getIdString());
    } catch (Exception ex) {
      envelope.put(Field.BAD_PACKET, rawPacketBytes);
      envelope.put(Field.BAD_EXCEPTION, ex);
      LOG.trace("Bad incoming packet {} in envelope #{}", rawPacketBytes, envelope.getIdString());
      ex.printStackTrace();
    }
    envelope.remove(Field.INCOMING);
  }
}
