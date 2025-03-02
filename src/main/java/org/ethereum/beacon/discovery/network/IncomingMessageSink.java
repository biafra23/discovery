/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

/**
 * Netty interface handler for incoming packets in form of raw bytes data wrapped as {@link Bytes}
 * Implementation forwards all incoming packets in {@link FluxSink} provided via constructor, so it
 * could be later linked to processor to form incoming messages stream
 */
public class IncomingMessageSink extends SimpleChannelInboundHandler<Envelope> {
  private static final Logger LOG = LoggerFactory.getLogger(IncomingMessageSink.class);
  private final FluxSink<Envelope> messageSink;

  public IncomingMessageSink(FluxSink<Envelope> messageSink) {
    this.messageSink = messageSink;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Envelope msg) {
    LOG.trace("Incoming packet {} in session {}", msg, ctx);
    messageSink.next(msg);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    LOG.error("Unexpected exception caught", cause);
  }
}
