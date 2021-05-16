contract a {
    function b() {
        bytes memory a = hex"00112233ff";
        bytes memory b = hex'00112233ff';
        uint x = 1.28e5 ether;
        uint y = 1e6;
        int z = -.5e+8;
        uint o = 2.E-5 ether;
        string astr = unicode"hello world";
        string bstr = "hello world";
        string cstr = "\xf0\x9f\xa6\x84";
        string dstr = unicode"😃, 😭,\
and 😈";
        //TODO
        //bytes32 escapeCharacters = unicode"foo" unicode"😃, 😭, and 😈" unicode"!";
    }
}
