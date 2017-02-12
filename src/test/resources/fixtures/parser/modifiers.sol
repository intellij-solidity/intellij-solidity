contract Purchase {
    modifier onlySeller() {
        _;
    }

    function abort() onlySeller {
    }
}
