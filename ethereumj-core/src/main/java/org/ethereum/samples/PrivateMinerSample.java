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
package org.ethereum.samples;

import com.typesafe.config.ConfigFactory;
import java.io.File;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.dpos.BPController;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.facade.EthereumImpl;
import org.ethereum.mine.EthashListener;
import org.ethereum.net.eth.handler.Eth62;
import org.ethereum.util.ByteUtil;
import org.iq80.leveldb.util.FileUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.Bean;

/**
 * The sample creates a small private net with two peers: one is the miner, another is a regular peer
 * which is directly connected to the miner peer and starts submitting transactions which are then
 * included to blocks by the miner.
 *
 * Another concept demonstrated by this sample is the ability to run two independently configured
 * EthereumJ peers in a single JVM. For this two Spring ApplicationContext's are created which
 * are mostly differed by the configuration supplied
 *
 * Created by Anton Nashatyrev on 05.02.2016.
 */
public class PrivateMinerSample {

    /**
     * Spring configuration class for the Miner peer
     */
    private static class MinerConfig {

        private final String config =
                // no need for discovery in that small network
                "peer.discovery.enabled = true ";

        @Bean
        public MinerNode node() {
            return new MinerNode();
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        public SystemProperties systemProperties() {
            SystemProperties props = new SystemProperties();
            props.overrideParams(ConfigFactory.parseString(config.replaceAll("'", "\"")));
            return props;
        }
    }

    /**
     * Miner bean, which just start a miner upon creation and prints miner events
     */
    static class MinerNode extends BasicSample implements EthashListener {
        public MinerNode() {
            // peers need different loggers
            super("MinerNode");
        }

        // overriding run() method since we don't need to wait for any discovery,
        // networking or sync events
        @Override
        public void run() {
            super.run();
            ethereum.getBlockMiner().addListener(this);
      //       ethereum.getBlockMiner().startMining();
            BPController controller =new BPController((EthereumImpl)ethereum);
            Eth62.setBPController(controller);
            controller.start();

        }

        @Override
        public void onDatasetUpdate(EthashListener.DatasetStatus minerStatus) {
            logger.info("Miner status updated: {}", minerStatus);
            if (minerStatus.equals(EthashListener.DatasetStatus.FULL_DATASET_GENERATE_START)) {
                logger.info("Generating Full Dataset (may take up to 10 min if not cached)...");
            }
            if (minerStatus.equals(DatasetStatus.FULL_DATASET_GENERATED)) {
                logger.info("Full dataset generated.");
            }
        }

        @Override
        public void miningStarted() {
            logger.info("Miner started");
        }

        @Override
        public void miningStopped() {
            logger.info("Miner stopped");
        }

        @Override
        public void blockMiningStarted(Block block) {
            logger.info("Start mining block: " + block.getShortDescr());
        }

        @Override
        public void blockMined(Block block) {
            logger.info("Block mined! : \n" + block);
        }

        @Override
        public void blockMiningCanceled(Block block) {
            logger.info("Cancel mining block: " + block.getShortDescr());
        }
    }


    /**
     *  Creating two EthereumJ instances with different config classes
     */
    public static void main(String[] args) throws Exception {
        //start a completely new chain
        File dbDir=new File("E:\\intellijProjects\\ethereumj-github-1.8.0 - 1\\sampleDB-1\\");
        for(File f:dbDir.listFiles()){
            if(!f.getName().endsWith("dat") && !f.getName().endsWith("properties"))
              FileUtils.deleteRecursively(f);
        }

        if (Runtime.getRuntime().maxMemory() < (1250L << 20)) {
            MinerNode.sLogger.error("Not enough JVM heap (" + (Runtime.getRuntime().maxMemory() >> 20) + "Mb) to generate DAG for mining (DAG requires min 1G). For this sample it is recommended to set -Xmx2G JVM option");
            return;
        }

        BasicSample.sLogger.info("Starting EthtereumJ miner instance!");
        Ethereum ethereum=EthereumFactory.createEthereum(MinerConfig.class);

//        BasicSample.sLogger.info("Starting EthtereumJ regular instance!");
//        EthereumFactory.createEthereum(RegularConfig.class);
    }
}
