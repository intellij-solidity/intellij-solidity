package me.serce.solidity.run;

import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.util.blockchain.EasyBlockchain;
import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static me.serce.solidity.run.ContractUtils.rethrow;


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

    private static SolidityContract submitAllContracts(String main, List<String> compiledContractDirs, EasyBlockchain init) throws IOException {
        System.out.print("Submitting contracts... ");
        AtomicReference<SolidityContract> res = new AtomicReference<>();
        compiledContractDirs.stream()
                .map(s -> Paths.get(s))
                .filter(p -> Files.isDirectory(p))
                .flatMap(p -> rethrow(() -> Files.walk(p)))
                .forEach(file -> {
                            SolContractMetadata solCm = ContractUtils.readContract(file);
                            if (solCm != null) {
                                CompilationResult.ContractMetadata cm = solCm.toEvmMetadata();
                                SolidityContract contract = init.submitNewContract(cm);
                                String fullName = file.getFileName().toString();
                        String fileName = fullName.substring(0, fullName.indexOf(".abi"));
                                if (fileName.equals(main)) {
                                    res.set(contract);
                                }
                            }
                        }
                );
        System.out.println(" Done.");
        return res.get();
    }

    public static void main(String[] args) {

        String mainContract = args[0];
        String function = args[1];
        List<String> compiledContractDirs = Arrays.asList(args).subList(2, args.length);

        try {
            StandaloneBlockchain init = init();
            SolidityContract contract = submitAllContracts(mainContract, compiledContractDirs, init);
            if (contract == null) {
                System.err.println("No contract found in the path: " + mainContract);
                return;
            }
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
