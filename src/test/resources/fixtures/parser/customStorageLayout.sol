contract C1 layout at 2**255 - 42 {
    uint x;
}

contract C2 layout at 1234 + 4567 {}

contract C3 layout at 0x1234 {
    uint layout;
    function at() public pure { }
}

contract C4 layout at 5 minutes { }
contract C5 layout at 2 gwei { }
