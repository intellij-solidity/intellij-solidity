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

    function iffun() {
        assembly {
            if eq(value, 0) { revert(0, 0) }
            if 42 {}
            if 42 { let x := 3 }
        }
    }

    function switchfun() {
        assembly {
            switch exponent
                case 0 { result := 1 }
                case 1 { result := base }
                default {
                    result := power(mul(base, base), div(exponent, 2))
                    switch mod(exponent, 2)
                    case 1 { result := mul(base, result) }
            }

            let a := 3 switch a case 1 { a := 1 } case 2 { a := 5 } a := 9
            let a := 2 switch calldataload(0) case 1 { a := 1 } case 2 { a := 5 }
        }
    }

    function switchfor() {
        assembly {
            for { let i := 0 } lt(i, x) { i := add(i, 1) } { y := mul(2, y) }
        }
    }

    function funcalls() {
        assembly {
            function f(x) -> y { switch x case 0 { y := 1 } default { y := mul(x, f(sub(x, 1))) }   }
            function f(x) -> y { a := 1 }
            function f() -> x, y { let x, y := f() }
            function f() -> x {} if f() { pop(f()) }
            let r := 2 function f() -> x, y { x := 1 y := 2} let a, b := f() b := r
            function f() -> z { let y := 2 }
            function f(a, b) -> x, y, z { y := a }
            function f() { g() } function g() { f() }
            function f(r, s) -> x { function g(a) -> b { } x := g(2) } let x := f(2, 3)
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

    function _copyToBytes(uint btsPtr, bytes memory tgt, uint btsLen) {
        assembly {
            let i := 0 // Start at arr + 0x20
            let words := div(add(btsLen, 31), 32)
            let rOffset := btsPtr
            let wOffset := add(tgt, 0x20)
        tag_loop:
            jumpi(end, eq(i, words))
            {
                let offset := mul(i, 0x20)
                mstore(add(wOffset, offset), mload(add(rOffset, offset)))
                i := add(i, 1)
            }
            jump(tag_loop)
        end:
            mstore(add(tgt, add(0x20, mload(tgt))), 0)
        }
    }
}
