contract C {
    function g() {
        var (x, b, y) = f();
        (x, y) = (2, 7);
        (x, y) = (y, x);
        (,data[3]) = f();
        (data.length,) = f();
        (x,) = (1,);
    }
}
