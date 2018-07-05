package org.ethereum.dpos;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.core.Block;

public class BPCandidate {
  public String address;
  public String nodeId;

  public long currentBestBlock =-1;

  public String host;
  public int port;

  public BPCandidate(String address, String nodeId, String host, int port){
    this.nodeId=nodeId;
    this.address = address;
    this.host= host;
    this.port= port;
  }

  @Override
  public String toString(){
    return address+":"+nodeId;
  }
}
