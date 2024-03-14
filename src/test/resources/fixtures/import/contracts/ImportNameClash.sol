import "./a/SimpleName.sol";

contract ImportUsage is SimpleName {

    function test() {
        abc();
       //^
    }
}
