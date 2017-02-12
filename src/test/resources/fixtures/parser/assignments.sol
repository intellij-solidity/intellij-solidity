contract C {
    function g() {
        var (x, b, y) = f();
        (x, y) = (2, 7);
        (x, y) = (y, x);
        (data.length,) = f();
        (,data[3]) = f();
        (x,) = (1,);
    }
}
