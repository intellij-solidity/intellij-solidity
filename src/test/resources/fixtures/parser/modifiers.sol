contract Purchase {
    modifier onlySeller() {
        _;
    }

    function abort() onlySeller {
    }

    receive() external onlySeller payable {}
}
