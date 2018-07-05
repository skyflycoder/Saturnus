package org.ethereum.dpos;

import org.ethereum.net.eth.message.EthMessage;
import org.ethereum.net.eth.message.EthMessageCodes;

public class CandidateMsg  extends EthMessage {
  private String msg;
  public CandidateMsg(String msg){
    this.msg = msg;
    parsed = true;
  }
  public CandidateMsg(byte[] encoded){
    super(encoded);
  }
  public String getMsg(){
    return msg;
  }


  private void encode() {
    this.encoded = msg.getBytes();
  }

  @Override
  public byte[] getEncoded() {
    if (encoded == null) encode();
    return encoded;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  private synchronized void parse() {
    if (parsed) return;
    msg=new String(encoded);
    parsed = true;
  }

  /**
   * Returns the message in String format
   *
   * @return A string with all attributes of the message
   */
  public String toString() {
    parse();
    return "CandidateMsg:"+msg;
  }

  public EthMessageCodes getCommand(){
    return EthMessageCodes.CANDIDATE_MSG;
  }

}
