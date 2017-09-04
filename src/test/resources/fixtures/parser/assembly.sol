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

    function fibo() {
        assembly {
            let n := calldataload(4)
            let a := 1
            let b := a
        loop:
            jumpi(loopend, eq(n, 0))
            a add swap1
            n := sub(n, 1)
            jump(loop)
        loopend:
            mstore(0, a)
            return(0, 0x20)
        }
    }
}
