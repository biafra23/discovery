/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.ethereum.beacon.discovery.packet.impl;

import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.packet.BytesSerializable;
import org.ethereum.beacon.discovery.util.DecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBytes implements BytesSerializable {

  private final Bytes bytes;
  private static final Logger LOG = LoggerFactory.getLogger(AbstractBytes.class);

  public static Bytes checkStrictSize(Bytes bytes, int expectedSize) throws DecodeException {
    if (bytes.size() != expectedSize) {
      throw new DecodeException(
          "FORK: Data size (" + bytes.size() + ") doesn't match expected: " + expectedSize);
    }
    LOG.warn("FORK: bytes.size(): (" + bytes.size() + ") == expectedSize: (" + expectedSize + ")");
    return bytes;
  }

  public static Bytes checkMinSize(Bytes bytes, int minimalSize) throws DecodeException {
    if (bytes.size() < minimalSize) {
      throw new DecodeException(
          "Data is too small: " + bytes.size() + ", (expected at least " + minimalSize + " bytes)");
    }
    LOG.warn("FORK: {} >= {}", bytes.size(), minimalSize);
    return bytes;
  }

  protected AbstractBytes(Bytes bytes) {
    this.bytes = bytes;
  }

  @Override
  public Bytes getBytes() {
    return bytes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractBytes that = (AbstractBytes) o;
    return Objects.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bytes);
  }
}
