contract C {
    int constant public transient x = 0;
}

struct S {
    int transient x;
}

contract C {
    S transient s;
}

contract C {
    mapping(uint => uint) transient y;
}

contract C {
    address transient payable a;
}

contract C {
    function f() public pure {
        uint transient x = 0;
    }
}

contract test {
    function f(bytes transient) public;
}

contract C {
    uint public immutable transient x;
}

contract C {
    function f(uint[] transient) private pure {}
}

contract A {
    modifier mod2(uint[] transient) { _; }
}

contract C {
    function f(uint[] transient transient x) public pure { }
}

contract C {
    function (uint transient) external y;
    function (uint[] transient) external z;
}

contract C {
    uint storage transient x;
}

contract C {
    uint transient storage x;
}

contract C {
    uint[3] transient x;
}

contract C {
    int transient x = -99;
    address transient a = address(0xABC);
    bool transient b = x > 0 ? false : true;
}

contract C {
    event e(string indexed transient a);
}

contract C {
    function h() public pure returns(uint[] transient) {}
}
