pragma solidity >=0.7.0 <0.9.0;

type RestrictedNumber is int256;

using {plusOne} for RestrictedNumber global;

library LibraryName {}
library Library2 {}
library Lib {}

contract C {
    struct s { uint a; }
    using LibraryName for uint;
    using Library2 for *;
    using Lib for s;
}


function plusOne(RestrictedNumber x) pure returns (RestrictedNumber) {
    unchecked {
        return RestrictedNumber.wrap(RestrictedNumber.unwrap(x) + 1);
    }
}