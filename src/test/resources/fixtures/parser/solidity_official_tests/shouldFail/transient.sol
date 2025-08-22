contract C1 {
    int constant public transient x = 0;
}

struct S1 {
    int transient x;
}

contract C2 {
    S1 transient s;
}

contract C3 {
    mapping(uint => uint) transient y;
}

contract C4 {
    address transient payable a;
}

contract C5 {
    address payable transient a;
}


contract C6 {
    function f() public pure {
        uint transient x = 0;
    }
}

contract test {
    function f(bytes transient) public;
}

contract C7 {
    uint public immutable transient x;
}

contract C8 {
    function f(uint[] transient) private pure {}
}

contract A {
    modifier mod2(uint[] transient) { _; }
}

contract C9 {
    function f(uint[] transient transient x) public pure { }
}

contract C10 {
    function (uint transient) external y;
    function (uint[] transient) external z;
}

contract C11 {
    uint storage transient x;
}

contract C12 {
    uint transient storage x;
}

contract C13 {
    uint[3] transient x;
}

contract C14 {
    int transient x = -99;
    address transient a = address(0xABC);
    bool transient b = x > 0 ? false : true;
}

contract C15 {
    event e(string indexed transient a);
}

contract C16 {
    function h() public pure returns(uint[] transient) {}
}
