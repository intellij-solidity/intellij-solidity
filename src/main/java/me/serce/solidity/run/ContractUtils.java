package me.serce.solidity.run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ContractUtils {

    public static SolContractMetadata readContract(Path file) {
        String fullName = file.getFileName().toString();
        if (fullName.endsWith(".abi")) {
            String fileName = fullName.substring(0, fullName.indexOf(".abi"));
            List<File> files = Arrays.asList(file.getParent().toFile().listFiles());
            File bin = new File(file.getParent().toFile(), fileName + ".bin");
            if (files.contains(bin)) {
                return SolContractMetadata.builder()
                        .abi(readFile(file))
                        .bin(readFile(bin.toPath()))
                        .abiFile(file.toFile())
                        .binFile(bin)
                        .build();
            }
        }
        return null;
    }

    private static String readFile(Path file) {
        return rethrow(() -> new String(Files.readAllBytes(file)));
    }

    static <R> R rethrow(ThrowableSupplier<R> block) {
        try {
            return block.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    interface ThrowableSupplier<R> {
        R get() throws IOException;
    }
}
