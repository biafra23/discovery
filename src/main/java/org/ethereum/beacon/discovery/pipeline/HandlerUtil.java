/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.pipeline;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerUtil {
  private static final Logger LOG = LoggerFactory.getLogger(HandlerUtil.class);

  public static boolean requireField(Field<?> field, Envelope envelope) {
    if (envelope.contains(field)) {
      return true;
    } else {
      LOG.trace(
          "Requirement not satisfied: field {} not exists in envelope {}",
          field,
          envelope.getIdString());
      return false;
    }
  }

  public static boolean requireSessionWithNodeRecord(Envelope envelope) {
    if (!requireField(Field.SESSION, envelope)) {
      return false;
    }
    if (envelope.get(Field.SESSION).getNodeRecord().isEmpty()) {
      LOG.trace(
          "Requirement not satisfied: node record unknown in envelope {}", envelope.getIdString());
      return false;
    }
    return true;
  }

  public static boolean requireCondition(
      Function<Envelope, Boolean> conditionFunction, Envelope envelope) {
    if (conditionFunction.apply(envelope)) {
      return true;
    } else {
      LOG.trace(
          "Requirement not satisfied: condition {} not met for envelope {}",
          conditionFunction,
          envelope.getIdString());
      return false;
    }
  }
}
