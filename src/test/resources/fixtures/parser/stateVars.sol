contract A {
    uint public a;
    uint public constant b = 1;
    uint private c;
    uint internal constant d = 1;
    uint constant e = 1;
    uint constant public f = 1;
    uint g = 1;
    uint immutable h = 1;
    uint internal immutable i = 1;
    uint immutable public j = 1;
    uint public override k;
    uint internal immutable override l = 1;
    uint constant public override m = 1;
}
