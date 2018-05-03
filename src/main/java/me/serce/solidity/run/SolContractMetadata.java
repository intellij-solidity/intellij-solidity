package me.serce.solidity.run;

import org.ethereum.solidity.compiler.CompilationResult;

import java.io.File;

public class SolContractMetadata {
    public final String abi;
    public final String bin;
    public final String solInterface;
    public final String metadata;

    public final File abiFile;
    public final File binFile;

    private SolContractMetadata(String abi, String bin, String solInterface, String metadata, File abiFile, File binFile) {
        this.abi = abi;
        this.bin = bin;
        this.solInterface = solInterface;
        this.metadata = metadata;
        this.abiFile = abiFile;
        this.binFile = binFile;
    }

    public CompilationResult.ContractMetadata toEvmMetadata() {
        CompilationResult.ContractMetadata cm = new CompilationResult.ContractMetadata();
        cm.bin = bin;
        cm.abi = abi;
        cm.metadata = metadata;
        cm.solInterface = solInterface;
        return cm;
    }


    public static SolContractMetadataBuilder builder() {
        return new SolContractMetadataBuilder();
    }

    public static final class SolContractMetadataBuilder {
        public String abi;
        public String bin;
        public String solInterface;
        public String metadata;
        public File abiFile;
        public File binFile;

        private SolContractMetadataBuilder() {
        }

        public static SolContractMetadataBuilder aSolContractMetadata() {
            return new SolContractMetadataBuilder();
        }

        public SolContractMetadataBuilder abi(String abi) {
            this.abi = abi;
            return this;
        }

        public SolContractMetadataBuilder bin(String bin) {
            this.bin = bin;
            return this;
        }

        public SolContractMetadataBuilder solInterface(String solInterface) {
            this.solInterface = solInterface;
            return this;
        }

        public SolContractMetadataBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public SolContractMetadataBuilder abiFile(File abiFile) {
            this.abiFile = abiFile;
            return this;
        }

        public SolContractMetadataBuilder binFile(File binFile) {
            this.binFile = binFile;
            return this;
        }

        public SolContractMetadata build() {
            SolContractMetadata solContractMetadata = new SolContractMetadata(abi, bin, solInterface, metadata, abiFile, binFile);
            return solContractMetadata;
        }
    }
}
