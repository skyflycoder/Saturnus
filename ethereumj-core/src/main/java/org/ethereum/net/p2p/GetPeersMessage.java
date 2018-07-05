/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.net.p2p;

import org.spongycastle.util.encoders.Hex;

/**
 * Wrapper around an Ethereum GetPeers message on the network
 *
 * @see org.ethereum.net.p2p.P2pMessageCodes#GET_PEERS
 */
public class GetPeersMessage extends P2pMessage {

    /**
     * GetPeers message is always a the same single command payload
     */
    private final static byte[] FIXED_PAYLOAD = Hex.decode("C104");

    @Override
    public byte[] getEncoded() {
        return FIXED_PAYLOAD;
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.GET_PEERS;
    }

    @Override
    public Class<PeersMessage> getAnswerMessage() {
        return null;
    }

    @Override
    public String toString() {
        return "[" + this.getCommand().name() + "]";
    }
}