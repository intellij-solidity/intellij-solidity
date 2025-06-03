contract C {
    address public immutable transient;
}

contract C {
    address payable transient a;
}

contract C {
    int constant public transient = 0;
}

contract C {
    uint public transient pubt;
    uint internal transient it;
    uint private transient prvt;

    uint transient public tpub;
    uint transient internal ti;
    uint transient private tprv;
}

contract C {
    function () transient f;
    function (uint) external transient y;
    function () transient internal fti;
    function () internal transient fit;
    function () internal transient internal fiti;
    function () internal internal transient fiit;
}

contract D { }

contract C {
    address transient a;
    bool transient b;
    D transient d;
    uint transient x;
    bytes32 transient y;
}

contract C {
function transient() public pure { }
}

error CustomError(uint transient);
event e1(uint transient);
event e2(uint indexed transient);

struct S {
    int transient;
}

contract D {
    function f() public pure returns (uint) {
    uint transient = 1;
    return transient;
}

function g(int transient) public pure { }

modifier m(address transient) {
    _;
    }
}
