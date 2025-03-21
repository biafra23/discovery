/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.util.Map;
import org.apache.tuweni.bytes.Bytes;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/** Netty discovery UDP client */
public class NettyDiscoveryClientImpl implements DiscoveryClient {
  private static final Logger LOG = LoggerFactory.getLogger(NettyDiscoveryClientImpl.class);

  private final Map<InternetProtocolFamily, NioDatagramChannel> channels;

  /**
   * Constructs UDP client using
   *
   * @param outgoingStream Stream of outgoing packets, client will forward them to the channel
   * @param channels must have either 1 entry (IPv4/IPv6) or 2 entries (IPv4 and IPv6)
   */
  public NettyDiscoveryClientImpl(
      final Publisher<NetworkParcel> outgoingStream,
      final Map<InternetProtocolFamily, NioDatagramChannel> channels) {
    this.channels = channels;
    Flux.from(outgoingStream)
        .subscribe(
            networkPacket ->
                send(networkPacket.getPacket().getBytes(), networkPacket.getDestination()));
    LOG.info("UDP discovery client started");
  }

  @Override
  public void stop() {}

  @Override
  public void send(final Bytes data, final InetSocketAddress destination) {
    final DatagramPacket packet =
        new DatagramPacket(Unpooled.copiedBuffer(data.toArray()), destination);
    final NioDatagramChannel channel =
        channels.get(InternetProtocolFamily.of(destination.getAddress()));
    if (channel == null) {
      LOG.trace("Dropping packet {} because of IP version incompatibility", packet);
      return;
    }
    LOG.trace("Sending packet {}", packet);
    channel.write(packet);
    channel.flush();
  }
}
