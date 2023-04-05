pragma solidity >=0.7.0 <0.9.0;

type RestrictedNumber is int256;

using {plusOne} for RestrictedNumber global;

using {
    Helpers.add as +,
    Helpers.and2 as &,
    Math.div as /,
    Helpers.eq as ==,
    Helpers.gt as >,
    Helpers.gte as >=,
    Helpers.lt as <,
    Helpers.lte as <=,
    Helpers.or as |,
    Helpers.mod as %,
    Math.mul as *,
    Helpers.neq as !=,
    Helpers.not as ~,
    Helpers.sub as -,
    Helpers.xor as ^
} for UD60x18 global;

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
