contract a {
    function b() {
        assembly {
            // before function
            function f(x) -> y {a := 1}
            /* before mstore */
            mstore(0x40, sload(0))
            byte(0)
            add(x, y)
            selfdestruct(a)
            keccak256(p, n)
            jump(label)
            sha3(p, n)
            number(label)
            address(0)
            return (0x40, 32)
        }
    }
}
