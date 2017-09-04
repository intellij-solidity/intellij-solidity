contract Foo {
    function Foo() {
        var a = 1;
        var b = 2;
        assembly { a := b }
        assembly { a := b() }
        assembly { =: b }
    }

    function get() constant returns(uint) {
        assembly { 2 3 add "abc" and }
        assembly {
            mstore(0x40, sload(0))
            byte(0)
            add(x, y)
            selfdestruct(a)
            keccak256(p, n)
            jump(label)
            sha3(p, n)
            number(label)
            address(0)
            return(0x40,32)
        }
    }
}
