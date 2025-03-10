/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.AddressAccessPolicy;
import org.ethereum.beacon.discovery.network.NetworkParcel;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

/**
 * Looks up for {@link NetworkParcel} in {@link Field#INCOMING} field. If it's found, it shows that
 * we have outgoing parcel at the very first stage. Handler pushes it to `outgoingSink` stream which
 * is linked with discovery client.
 */
public class OutgoingParcelHandler implements EnvelopeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(OutgoingParcelHandler.class);

  private final FluxSink<NetworkParcel> outgoingSink;
  private final AddressAccessPolicy addressAccessPolicy;

  public OutgoingParcelHandler(
      FluxSink<NetworkParcel> outgoingSink, final AddressAccessPolicy addressAccessPolicy) {
    this.outgoingSink = outgoingSink;
    this.addressAccessPolicy = addressAccessPolicy;
  }

  @Override
  public void handle(Envelope envelope) {
    if (!HandlerUtil.requireField(Field.INCOMING, envelope)) {
      return;
    }
    LOG.trace(
        "Envelope {} in OutgoingParcelHandler, requirements are satisfied!",
        envelope.getIdString());

    if (envelope.get(Field.INCOMING) instanceof NetworkParcel) {
      NetworkParcel parcel = (NetworkParcel) envelope.get(Field.INCOMING);
      if (parcel.getPacket().getBytes().size() > IncomingDataPacker.MAX_PACKET_SIZE) {
        LOG.error("Outgoing packet is too large, dropping it: {}", parcel.getPacket());
      } else if (!addressAccessPolicy.allow(parcel.getDestination())) {
        LOG.debug(
            "Dropping outgoing packet to disallowed destination: {}", parcel.getDestination());
      } else {
        outgoingSink.next(parcel);
        envelope.remove(Field.INCOMING);
      }
    }
  }
}
