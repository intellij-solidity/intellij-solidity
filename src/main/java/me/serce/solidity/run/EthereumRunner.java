package me.serce.solidity.run;

import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.util.blockchain.EasyBlockchain;
import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;



public class EthereumRunner {

    private static StandaloneBlockchain init() {
        SystemProperties.getDefault().setBlockchainConfig(new FrontierConfig(new FrontierConfig.FrontierConstants() {
            @Override
            public BigInteger getMINIMUM_DIFFICULTY() {
                return BigInteger.ONE;
            }
        }));
        String dbDirProp = System.getProperty("evm.database.dir");
        if (dbDirProp != null) {
            SystemProperties.getDefault().setDataBaseDir(dbDirProp);
        }
        StandaloneBlockchain blockchain = new StandaloneBlockchain().withAutoblock(true);
        System.out.println("Creating first empty block (need some time to generate DAG)...");
        blockchain.createBlock();
        System.out.println("Done.");
        return blockchain;
    }

    private static SolidityContract readAllContracts(String main, Path dir, EasyBlockchain init) throws IOException {
        List<Path> contracts = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".sol")) {
                    contracts.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.print("Submitting contracts... ");
        StringBuilder sb = new StringBuilder();
        for (Path cp : contracts) {
            sb.append(new String(Files.readAllBytes(cp)));
        }
        SolidityContract res = init.submitNewContract(sb.toString(), main);

        System.out.println(" Done");
        return res;
    }


    public static void main(String[] args) throws IOException {

        String mainContract = args[0];
        String function = args[1];
        Path dir = Paths.get(args[2]);

        try {
            StandaloneBlockchain init = init();
            SolidityContract contract = readAllContracts(mainContract, dir, init);

            Object string = contract.callFunction(function).getReturnValue();
            System.out.println(String.format("Function '%s.%s' returned:", mainContract, args[1]));
            System.out.println(string);
        } catch (Exception e) {
            System.out.println("Exception occurred: " + e.getMessage());
        } finally {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }
}
