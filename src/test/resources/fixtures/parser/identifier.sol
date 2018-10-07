contract A {
    function _preValidatePurchase(
        address beneficiary,
        uint256 weiAmount
    )
    internal
    {
        super._preValidatePurchase(beneficiary, weiAmount);
    }
}
