contract C {
    function g() {
        var (x, b, y) = f();
        (x, y) = (2, 7);
        (x, y) = (y, x);
        (data.length,) = f(); // Sets the length to 7
        (,data[3]) = f(); // Sets data[3] to 2
        (x,) = (1,);
    }
}
