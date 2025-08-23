contract A {
    function a() {
        bytes exampleBytes = '0xabcd';
        exampleBytes[2:5];  // 'abc'
        exampleBytes[:5];   // '0xabc'
        exampleBytes[2:];   // 'abcd'
        exampleBytes[:];    // '0xabcd'
    }
}
