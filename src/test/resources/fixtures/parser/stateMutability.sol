contract A {
    function f1(uint a, uint b) pure returns (uint) {
        return a * (b + 42);
    }

    function f3(uint a, uint b) view returns (uint) {
        return a * (b + 42);
    }

    function f4(uint a, uint b) payable returns (uint) {
        return a * (b + 42);
    }
}
