package me.serce.solidity.run;

import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.util.blockchain.EasyBlockchain;
import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


// NOTE: java 7 language level
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

    private static SolidityContract submitAllContracts(String main, List<String> compiledContracts, EasyBlockchain init) throws IOException {
        System.out.print("Submitting contracts... ");
        StringBuilder sb = new StringBuilder();
        for (String cc : compiledContracts) {
            sb.append(new String(Files.readAllBytes(Paths.get(cc))));
        }
        SolidityContract res = init.submitNewContractFromJson(sb.toString(), main);

        System.out.println(" Done.");
        return res;
    }


    public static void main(String[] args) {

        String mainContract = args[0];
        String function = args[1];
        List<String> compiledContracts = Arrays.asList(args).subList(2, args.length);

        try {
            StandaloneBlockchain init = init();
            SolidityContract contract = submitAllContracts(mainContract, compiledContracts, init);
            Object result = contract.callFunction(function).getReturnValue();
            System.out.println(String.format("Function '%s.%s' returned:", mainContract, args[1]));
            System.out.println(resultToString(result));
        } catch (Throwable e) {
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
