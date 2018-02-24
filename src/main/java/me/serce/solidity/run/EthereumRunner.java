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
import java.util.Arrays;
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

    private static SolidityContract submitAllContracts(String main, List<String> dirs, EasyBlockchain init) throws IOException {
        List<Path> contracts = new ArrayList<>();
        for (String dir : dirs) {
            Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".sol")) {
                        contracts.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        System.out.print("Submitting contracts... ");
        StringBuilder sb = new StringBuilder();
        for (Path cp : contracts) {
            sb.append(new String(Files.readAllBytes(cp)));
        }
        SolidityContract res = init.submitNewContract(sb.toString(), main);

        System.out.println(" Done");
        return res;
    }


    public static void main(String[] args) {

        String mainContract = args[0];
        String function = args[1];
        List<String> sources = Arrays.asList(args).subList(2, args.length);

        try {
            StandaloneBlockchain init = init();
            SolidityContract contract = submitAllContracts(mainContract, sources, init);
            Object result = contract.callFunction(function).getReturnValue();
            System.out.println(String.format("Function '%s.%s' returned:", mainContract, args[1]));
            System.out.println(resultToString(result));
        } catch (Exception e) {
            System.err.println("\nException occurred while calling contract: " + e.getMessage());
        } finally {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }

    private static String resultToString(Object result) {
        if (result == null) {
            return "null";
        }
        return result.getClass().isArray() ? Arrays.toString((Object[]) result) : result.toString();
    }
}
