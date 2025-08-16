contract A {
    function a() public pure {
        b(hex"");
    }

    function b(string memory b) public pure {
    }
}
