contract A {
    function _callERC165SupportsInterface(address account, bytes4 interfaceId) private view returns (bool, bool)
    {
        bytes memory encodedParams = abi.encodeWithSelector(_INTERFACE_ID_ERC165, interfaceId);
        (bool success, bytes memory result) = account.staticcall{ gas: 30000 }(encodedParams);
        return (success, abi.decode(result, (bool)));
    }
}
