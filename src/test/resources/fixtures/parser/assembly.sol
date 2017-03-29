contract Foo {
    function Foo() {
        var a = 1;
        var b = 2;
        assembly { a := b }
    }
}
