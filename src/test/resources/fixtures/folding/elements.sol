<fold text='contract Tree  {...} '>contract Tree {
    <fold text='function myFunc(
        int a
    )  {...} '>function myFunc(
        int a
    ) <fold text='{...}'>{
        int line1 = 1;
        int line2 = 2;
    }</fold></fold>

}</fold>

<fold text='enum MyEnum  {...} '>enum MyEnum {
    Val1,
    Val2
}</fold>
<fold text='struct AssetPair  {...} 'struct AssetPair {
    address assetLeft;
    address assetRight;
}
