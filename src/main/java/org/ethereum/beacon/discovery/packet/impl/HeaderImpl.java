/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.ethereum.beacon.discovery.packet.impl;

import static com.google.common.base.Preconditions.checkArgument;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.packet.AuthData;
import org.ethereum.beacon.discovery.packet.Header;
import org.ethereum.beacon.discovery.packet.StaticHeader;
import org.ethereum.beacon.discovery.packet.impl.HandshakeMessagePacketImpl.HandshakeAuthDataImpl;
import org.ethereum.beacon.discovery.packet.impl.OrdinaryMessageImpl.OrdinaryAuthDataImpl;
import org.ethereum.beacon.discovery.packet.impl.WhoAreYouPacketImpl.WhoAreYouAuthDataImpl;
import org.ethereum.beacon.discovery.type.Bytes16;
import org.ethereum.beacon.discovery.util.CryptoUtil;
import org.ethereum.beacon.discovery.util.DecodeException;
import org.ethereum.beacon.discovery.util.DecryptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderImpl<TAuthData extends AuthData> extends AbstractBytes
    implements Header<TAuthData> {

  private static final Logger LOG = LoggerFactory.getLogger(HeaderImpl.class);

  public static Header<?> decrypt(Bytes data, Bytes16 iv, Bytes16 destNodeId)
      throws DecodeException {
    try {
      checkMinSize(data, StaticHeaderImpl.STATIC_HEADER_SIZE);
      LOG.debug("FORK: after checkMinSize");
      Cipher cipher = CryptoUtil.createAesctrDecryptor(destNodeId, iv);
      LOG.debug("FORK: cipher: " + cipher);
      LOG.debug("FORK: cipher.provider: " + cipher.getProvider());
      LOG.debug("FORK: cipher.algorithm: " + cipher.getAlgorithm());
      LOG.debug("FORK: cipher.blocksize: " + cipher.getBlockSize());
      LOG.debug("FORK: cipher.parameters: " + cipher.getParameters());
      Bytes staticHeaderCiphered = data.slice(0, StaticHeaderImpl.STATIC_HEADER_SIZE);
      LOG.debug("FORK: before decode: staticHeaderCiphered.size: {}", staticHeaderCiphered.size());
      Bytes staticHeaderBytes = Bytes.wrap(cipher.update(staticHeaderCiphered.toArrayUnsafe()));
      LOG.debug("FORK: before decode: staticHeaderBytes.size: {}", staticHeaderBytes.size());
      StaticHeader header = StaticHeader.decode(staticHeaderBytes);
      header.validate();

      int authDataSize = header.getAuthDataSize();
      int headerSize = StaticHeaderImpl.STATIC_HEADER_SIZE + authDataSize;
      checkMinSize(data, headerSize);
      Bytes authDataCiphered = data.slice(StaticHeaderImpl.STATIC_HEADER_SIZE, authDataSize);
      Bytes authDataBytes = Bytes.wrap(cipher.doFinal(authDataCiphered.toArrayUnsafe()));
      AuthData authData = decodeAuthData(header, authDataBytes);
      return new HeaderImpl<>(header, authData);
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      throw new DecryptException("Error decrypting packet header auth data", e);
    } catch (Exception e) {
      throw new DecodeException(
          "Couldn't decode header (iv=" + iv + ", nodeId=" + destNodeId + "): " + data, e);
    }
  }

  private static AuthData decodeAuthData(StaticHeader header, Bytes authDataBytes) {
    switch (header.getFlag()) {
      case WHOAREYOU:
        return new WhoAreYouAuthDataImpl(authDataBytes);
      case HANDSHAKE:
        return new HandshakeAuthDataImpl(authDataBytes);
      case MESSAGE:
        return new OrdinaryAuthDataImpl(authDataBytes);
      default:
        throw new DecodeException("Unknown flag: " + header.getFlag());
    }
  }

  private final StaticHeader staticHeader;
  private final TAuthData authData;

  public HeaderImpl(StaticHeader staticHeader, TAuthData authData) {
    super(Bytes.wrap(staticHeader.getBytes(), authData.getBytes()));
    checkArgument(
        authData.getBytes().size() == staticHeader.getAuthDataSize(),
        "Actual authData size doesn't match header auth-data-size field");
    this.staticHeader = staticHeader;
    this.authData = authData;
  }

  @Override
  public int getSize() {
    return StaticHeaderImpl.STATIC_HEADER_SIZE + getAuthDataBytes().size();
  }

  @Override
  public StaticHeader getStaticHeader() {
    return staticHeader;
  }

  @Override
  public TAuthData getAuthData() {
    return authData;
  }

  private Bytes getAuthDataBytes() {
    return authData.getBytes();
  }

  @Override
  public String toString() {
    return "Header{header=" + staticHeader + ", authData=" + authData + "}";
  }
}
