contract c {
    function f() returns (uint, uint) {
        return (1, 1);
    }

    function b() returns (int, int, int) {
        int result1;
        (result1,, ) = aaa();
    }
}
