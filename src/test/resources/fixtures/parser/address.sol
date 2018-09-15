contract C1 {
    address payable constant a = address(0);

    function f() public pure {
        address payable b = address payable(this);
    }

    function (address payable) view internal returns (address payable) f;
    function g(function (address payable) payable external returns (address payable)) public payable returns (function (address payable) payable external returns (address payable)) {
        function (address payable) payable external returns (address payable) h; h;
    }
}

library L {
}

contract C2 {
    using L for address payable;
}

contract C3 {
    struct S {
        address payable a;
        address payable[] b;
        mapping(uint => address payable) c;
        mapping(uint => address payable[]) d;
    }
    
    mapping(uint => address payable) m;
    mapping(uint => address payable[]) n;
    function f() public view {
        address payable a;
        address payable[] memory b;
        mapping(uint => address payable) storage c = m;
        mapping(uint => address payable[]) storage d = n;
        a; b; c; d;
    }
    
    function f() public pure returns(address payable[] memory m) {
        m = new address payable[](10);
    }
}

