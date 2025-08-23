contract Test {
    function f(uint a, uint[4] memory b) internal {
        b[0] = a;
    }

    function callerF1() internal {
        uint b = 1;
        bool c = false;
        f(
            4,
            [
                c ? 1 : 0,
                b,
                5,
                b == 2 ? 1 : 0
            ]
        );
    }

    function callerF2() internal {
        uint b = 1;
        bool c = false;
        f(4, [c ? 1 : 0, b, 5, b == 2 ? 1 : 0]);
    }
}
