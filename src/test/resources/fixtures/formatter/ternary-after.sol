contract Ternary {
    function f(uint p_) internal view returns (uint, uint, uint) {
        uint a = 1;
        uint b = 3;

        uint c = a > b ? a : b;
        uint d = a > c
            ? a
            : b;

        uint e = a < d
            ? c > b
                ? a
                : b
            : c;

        f(
            a + b < c
                ? d
                : e + c < b
                    ? e
                    : p_
        );

        return (
            a > b
                ? d
                : d < b
                    ? e
                    : 15,
            a + b < c
                ? d
                : e + c < b
                    ? e
                    : p_,
            a > b ? d : e
        );
    }
}
