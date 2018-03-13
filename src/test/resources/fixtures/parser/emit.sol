contract C {
    event e();

    function f() public {
        emit e();
    }

    event e2(uint a, string b);

    function f() public {
        emit e2(2, "abc");
        emit e2({b : "abc", a : 8});
    }
}

contract A {
    event e3(uint a, string b);
}

contract C2 is A {
    function f() public {
        emit A.e3(2, "abc");
        emit A.e3({b : "abc", a : 8});
    }
}

