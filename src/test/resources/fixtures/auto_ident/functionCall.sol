contract A {
    function b(uint256 x, uint256 y) {
    }

    function a() {
        b(
            100, <caret>
        );
        b(<caret>);
    }
}
