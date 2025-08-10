contract A {
    struct Hey {
        uint256 x;
        uint256 y;
    }
    
    function a(){
        Hey hey = Hey({
            x: 1, 
            <caret>
        });
    }
}
