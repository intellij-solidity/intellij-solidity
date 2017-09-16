contract A {
    function test() private returns (uint) {
        if (amount < 0) {<caret>}
    }
}
