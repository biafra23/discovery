/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.message.handler;

import java.util.concurrent.CompletableFuture;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.TalkHandler;
import org.ethereum.beacon.discovery.message.TalkReqMessage;
import org.ethereum.beacon.discovery.message.TalkRespMessage;
import org.ethereum.beacon.discovery.schema.NodeRecord;
import org.ethereum.beacon.discovery.schema.NodeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TalkReqHandler implements MessageHandler<TalkReqMessage> {
  private static final Logger LOG = LoggerFactory.getLogger(TalkReqHandler.class);

  private final TalkHandler appTalkHandler;

  public TalkReqHandler(TalkHandler appTalkHandler) {
    this.appTalkHandler = appTalkHandler;
  }

  @Override
  public void handle(TalkReqMessage message, NodeSession session) {
    final NodeRecord srcNode = session.getNodeRecord().orElseThrow();
    CompletableFuture<Bytes> response =
        appTalkHandler.talk(srcNode, message.getProtocol(), message.getRequest());
    response
        .thenAccept(
            respBytes -> {
              TalkRespMessage respMessage = new TalkRespMessage(message.getRequestId(), respBytes);
              session.sendOutgoingOrdinary(respMessage);
            })
        .exceptionally(
            err -> {
              LOG.debug("Application TalkHandler completed with error", err);
              return null;
            });
  }
}
