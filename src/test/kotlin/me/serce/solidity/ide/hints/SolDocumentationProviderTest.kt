package me.serce.solidity.ide.hints

import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language


class SolDocumentationProviderTest : SolTestBase() {
  fun testDocumentation() = checkByText("""
          contract A {
          
              function main() {
                  withDoc/*caret*/();
              }
              /**
                my docs
              */
              function withDoc(int myInti, bool aBool) returns (uint) {
                 return 1;
              }
          }
      """, """<div class='definition'><pre><b style='color:rgb(0,0,128)'>function</b> withDoc(<b style='color:rgb(0,0,128)'>int</b> myInti, <b style='color:rgb(0,0,128)'><b style='color:rgb(0,0,128)'>bool</b></b> aBool)  <b style='color:rgb(0,0,128)'>returns</b> (<b style='color:rgb(0,0,128)'>uint</b>)</pre></div><div class='content'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;my&nbsp;docs</div>""")

  fun testDocsForBuiltinFunction() = checkByText("""
            contract A {
                function main() {
                    abi.encode/*caret*/();
                }
            }
        """, """<div class='definition'><pre><b style='color:rgb(0,0,128)'>function</b> encode(<b style='color:rgb(0,0,128)'>data</b>)  <b style='color:rgb(0,0,128)'>returns</b> (<b style='color:rgb(0,0,128)'>bytes</b> <b style='color:rgb(0,0,128)'>memory</b>)</pre></div><div class='content'>&nbsp;ABI-encodes&nbsp;the&nbsp;given&nbsp;arguments</div>""")

  fun testTripleSlashDocs() = checkByText("""
          contract A {
          
              function main() {
                  withDoc/*caret*/();
              }
              /// @notice my docs
              function withDoc(int myInti, bool aBool) returns (uint) {
                 return 1;
              }
          }
      """, """<div class='definition'><pre><b style='color:rgb(0,0,128)'>function</b> withDoc(<b style='color:rgb(0,0,128)'>int</b> myInti, <b style='color:rgb(0,0,128)'><b style='color:rgb(0,0,128)'>bool</b></b> aBool)  <b style='color:rgb(0,0,128)'>returns</b> (<b style='color:rgb(0,0,128)'>uint</b>)</pre></div><div class='content'>&nbsp;<span&nbsp;class='grayed'>notice:</span>&nbsp;my&nbsp;docs</div>""")

  fun testInheritedDocs() = checkByText("""
          contract Base {
              /// @notice base docs
              function withDoc(int a) { }
          }
          contract Child is Base {
              /// @inheritdoc
              function withDoc(int a) { }
              function main() {
                  withDoc/*caret*/(1);
              }
          }
      """, """<div class='definition'><pre><b style='color:rgb(0,0,128)'>function</b> withDoc(<b style='color:rgb(0,0,128)'>int</b> a)  </pre></div><div class='content'>&nbsp;<span&nbsp;class='grayed'>notice:</span>&nbsp;base&nbsp;docs<br/>&nbsp;<span&nbsp;class='grayed'>inheritdoc:</span></div>""")

  fun testParameterDocs() = checkByText("""
          contract A {
              /// @param myInt parameter docs
              function withDoc(uint myInt/*caret*/) { }
          }
      """, """<div class='definition'><pre><b style='color:rgb(0,0,128)'>uint</b> myInt</pre></div><div class='content'><span&nbsp;class='grayed'>param:</span>&nbsp;myInt&nbsp;parameter&nbsp;docs</div>""")

  fun testErrorDefinitionDocs() = checkByText("""
          contract A {
              error NotEnoughBalance(uint requested, uint available);
              function main() {
                  revert NotEnoughBalance/*caret*/(1, 2);
              }
          }
      """, """<div class='definition'><pre><b style='color:rgb(0,0,128)'>error</b> NotEnoughBalance (uint requested, uint available)</pre></div>""")

  private fun checkByText(@Language("Solidity") code: String, @Language("HTML") expectedDoc: String) {
    myFixture.configureByText("main.sol", code.replace("/*caret*/", "<caret>"))
    val originalElement = myFixture.getElementAtCaret()
    val element = originalElement.reference?.resolve() ?: originalElement
    val documentationProvider = SolDocumentationProvider()
    val generateDoc = documentationProvider.generateDoc(element, originalElement)
    assertNotNull(generateDoc)
    assertSameLines(expectedDoc, generateDoc!!)
  }

}
