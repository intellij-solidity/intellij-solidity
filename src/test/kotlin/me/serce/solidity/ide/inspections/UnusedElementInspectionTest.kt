package me.serce.solidity.ide.inspections

class UnusedElementInspectionTest : SolInspectionsTestBase(UnusedElementInspection()) {

  fun testUnusedImport() {
    InlineFile( code = """
            contract A {
              
            }
          """,
          name = "A.sol")
    InlineFile( code = """
            contract B {
              
            }
          """,
          name = "B.sol")
    checkByText("""
        /*@weak_warning descr="Unused import directive"@*/import "A.sol";/*@/weak_warning@*/
        import "B.sol";
        contract C is B {
        }      
    """, checkWeakWarn = true)
  }

  fun testUnusedElements() = checkByText("""
        int constant /*@weak_warning descr="Constant 'a' is never used"@*/a/*@/weak_warning@*/ = 1;
        int constant aa = 42;
    
        contract a {
            enum /*@weak_warning descr="Enum 'E' is never used"@*/E/*@/weak_warning@*/ {E1}
            enum EE {E2}
            struct /*@weak_warning descr="Struct 'S' is never used"@*/S/*@/weak_warning@*/ {
               int /*@weak_warning descr="Variable 's' is never used"@*/s/*@/weak_warning@*/;             
            }
            struct SS {
               int ss;             
            }

            uint /*@weak_warning descr="State variable 'b' is never used"@*/b/*@/weak_warning@*/;
            uint bb; 
            
            function /*@weak_warning descr="Function 'a' is never used"@*/a/*@/weak_warning@*/(SS ss, uint /*@weak_warning descr="Parameter 'ui' is never used"@*/ui/*@/weak_warning@*/) mm returns (int) {
                int /*@weak_warning descr="Variable 'i' is never used"@*/i/*@/weak_warning@*/ = bb;
                int ii = bb;
                ii = aa;
                ii = ss.ss; 
                ii = ss.ss;
                EE.E2;
                aa();
                return ii;
            }
            
            function aa() {
            
            }

            modifier /*@weak_warning descr="Modifier 'm' is never used"@*/m/*@/weak_warning@*/() {
                _;
            }
            modifier mm() {
                _;
            }
        }
    """, checkWeakWarn = true)
}
