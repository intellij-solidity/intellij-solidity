contract C {
    function sum1(uint a, uint b, uint c) returns (uint s) {
        s = a.add(b)
        .add(c);
        return s;
    }

    function sum2(uint a, uint b, uint c) returns (uint s) {
        return a.add(b)
        .add(c);
    }
}
