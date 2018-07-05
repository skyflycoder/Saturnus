package org.ethereum.dpos;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.ImportResult;
import org.ethereum.core.Transaction;
import org.ethereum.facade.EthereumImpl;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;


public class BPController {
  private static final Logger logger = LoggerFactory.getLogger("BPController");
  private static final List<BPCandidate> candidates = new ArrayList<>();
  private Thread produceThread;
  private volatile boolean enableRunnig = false;
  private EthereumImpl ethereum;

  public BPController(EthereumImpl ethereum) {
    this.ethereum = ethereum;
    produceThread = new Thread(produceLoop);

    List<String[]> list= getAllBlockProduceCandidates().subList(0,candidateCnt());
    List<String[]> hosts=allCandidateHosts().subList(0,candidateCnt());
    for(int i=0;i<candidateCnt();i++){
      String[] nodeId=list.get(i);
      String[] host=hosts.get(i);
      candidates.add(new BPCandidate(nodeId[1],nodeId[2],host[0],Integer.parseInt(host[1])));
    }
  }


  /**
   * receive block mined from any other candidate
   * @param b
   */
  public void onReceiveBlock(Block b){
    ethereum.receiveNewMinedBlockFromOtherCandidate(b);
  }

  public void sendCandidateMsg(CandidateMsg msg){
    ethereum.getChannelManager().sendCandidateMsg(msg);
  }
  //TODO: json
  public void onReceiveCandidateMsg(CandidateMsg candidateMsg){
    String msg=candidateMsg.getMsg();
    String address=msg.substring(0,40);
    int index=getIndex(address);
    if(index < 0)
      return;
    String sub=msg.substring(msg.indexOf("BestBlock"));
    int s=sub.indexOf("="),e=sub.indexOf(";");
    long bestBlock=Long.parseLong(sub.substring(s+1,e));
    candidates.get(index).currentBestBlock = bestBlock;
  }


  /**
   * Cycle thread to generate blocks
   */
  private Runnable produceLoop =
      () -> {
        long bestBlockNum=-1;
        logger.info("start coordinate candidates...");
        while(true) {
          bestBlockNum=ethereum.getBlockchain().getBestBlock().getNumber();
          candidates.get(myIndex()).currentBestBlock=bestBlockNum;
          String msg=candidates.get(myIndex()).address+",BestBlock="+ bestBlockNum+";";
          sendCandidateMsg(new CandidateMsg(msg));
          boolean coor=true;
          for(BPCandidate c:candidates){
            if(c.currentBestBlock<0 )
              coor=false;
          }
          if (coor) {
            for (BPCandidate c : candidates) {
              if (c.currentBestBlock != bestBlockNum)
                coor = false;
            }
          }
          try {
            if (!coor) {
              logger.info("waiting coordinate...");
              Thread.sleep(2000);

            } else {
              ethereum.getChannelManager().onSyncDone(true);
              ethereum.getSyncManager().makeSyncDone();
              logger.info("coordinate finished! bestBlockNum = " + bestBlockNum);
              logger.info("Will sleep 3 seconds before producing blocks.");
              Thread.sleep(3000);
              break;
            }

          } catch (Exception e) {
            logger.info(e.toString());
          }
        }


        while (enableRunnig) {
          long current=System.currentTimeMillis();
          try {
            Block bestBlock = ethereum.getBlockchain().getBestBlock();
            if (bestBlock.getNumber() == 0 && myIndex() == 0) {
              logger.info("tryProduceBlock, current is" + current + ", previous block is genesis block");

              List<Transaction> txs = ethereum.getPendingStateImpl().getPendingTransactions();
              tryProduceBlock(bestBlock, txs, new ArrayList<BlockHeader>());
              continue;
            }

            String addr = Hex.toHexString(bestBlock.getHeader().getCoinbase());
            int bestBlockIndex = candidateIndex(addr);

            if (bestBlockIndex < 0) {
              throw new RuntimeException("In produceLoop:Candidate not found.");
            }
            long bestBlockTime = bestBlock.getTimestamp() * 1000;



            int distance = bestBlockIndex == myIndex() ? candidates.size() : (myIndex() - bestBlockIndex + candidates.size()) % candidates.size();
            System.err.println(bestBlockIndex + ":::" + myIndex()+":::"+ candidates.size() + ":::" + (current - bestBlockTime)+"  distance="+distance);

            if (current - bestBlockTime > getBlockProduceIntervalMillisecs() * distance) {
              logger.info("tryProduceBlock, current is" + current + ", bestBlockTime is " + bestBlockTime);
              List<Transaction> txs = ethereum.getPendingStateImpl().getPendingTransactions();
              tryProduceBlock(bestBlock, txs, new ArrayList<BlockHeader>());
            } else {
              logger.info("Sleep 1000ms, current is" + current + ", bestBlockTime is " + bestBlockTime);
            }


          } catch (Exception ex) {
            logger.error("exception happened in produceLoop", ex);
          } catch (Throwable throwable) {
            logger.error("unknown throwable happened in produceLoop", throwable);
          } finally {
            try {
              Thread.sleep(1000);
            }catch (Exception e){}
          }
        }
      };

  private void tryProduceBlock(Block parent, List<Transaction> txs, List<BlockHeader> uncles){
    Block newBlock= ((BlockchainImpl)ethereum.getBlockchain()).createNewBlock(parent, txs,uncles);

    logger.debug("Importing newly mined block {} {} ...", newBlock.getShortHash(), newBlock.getNumber());
    ImportResult importResult = ethereum.addNewMinedBlock(newBlock);
    System.err.println("Mined block import result is " + importResult);
    System.err.println("Block mined! : \n" + newBlock);
    System.err.println("balance=" + ethereum.getRepository().getBalance(Hex.decode(candidates.get(myIndex()).address)));
  }

  private int getBlockProduceIntervalMillisecs() {
    return ethereum.getConfig().getBlockProduceIntervalMillisecs();
  }

  private List<String[]> getAllBlockProduceCandidates() {
    return ethereum.getConfig().getAllBlockProduceCandidates();
  }

  private int getIndex(String addr){
    for(int i=0;i<candidates.size();i++){
      if(candidates.get(i).address.equalsIgnoreCase(addr))
        return i;
    }
    return -1;
  }

  private int myIndex() {
    return ethereum.getConfig().candidateIndexOfMe();
  }

  private int candidateCnt() {
    return ethereum.getConfig().candidateCnt();
  }

  public int candidateIndex(String address) {
    for (int i = 0; i < candidates.size(); i++) {
      if (candidates.get(i).address.equalsIgnoreCase(address))
        return i;
    }
    return -1;
  }
  public List<String[]> allCandidateHosts(){
    return ethereum.getConfig().allCandidateHosts();
  }

  public void start() {
    enableRunnig = true;
    produceThread.start();
  }


  public void stop() {
    enableRunnig = false;
    produceThread.interrupt();
  }

}
