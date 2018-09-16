contract C {
    function f() public pure {
        uint d1 = 654_321;
        uint d2 =  54_321;
        uint d3 =   4_321;
        uint d4 = 5_43_21;
        uint d5 = 1_2e10;
        uint d6 = 12e1_0;
        d1; d2; d3; d4; d5; d6;
    }

    function f() public pure {
        uint x1 = 0X8765_4321;
        uint x2 = 0X765_4321;
        uint x3 = 0x65_4321;
        uint x4 = 0x5_4321;
        uint x5 = 0x123_1234_1234_1234;
        uint x6 = 0x123456_1234_1234;
        x1; x2; x3; x4; x5; x6;
    }
}
