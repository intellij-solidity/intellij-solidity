interface A {}

contract B {
    bytes4 private constant INT = type(A).interfaceId;
}
