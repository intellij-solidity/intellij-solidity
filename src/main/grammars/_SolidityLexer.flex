package me.serce.solidity;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static me.serce.solidity.lang.core.SolidityTokenTypes.*;

%%

%{
  public _SolidityLexer() {
    this((java.io.Reader)null);
  }
%}

%{
  // indicates the start of the comment
  private int zzBlockCommentStarted = -1;
%}

%public
%class _SolidityLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%state PRAGMA_OPEN
%state PRAGMA_REST

EOL=\R
%state IN_BLOCK_COMMENT
%state IN_EOL_COMMENT

WHITE_SPACE=\s+

EOL_COMMENT="/""/"[^\n]*
// see https://docs.soliditylang.org/en/v0.8.7/natspec-format.html for NatSpec tags support
NAT_SPEC_TAG=@[a-zA-Z_0-9:]*
HEXLITERAL=(hex\"([_0-9a-fA-F]+)\"|hex\'([_0-9a-fA-F]+)\')
STRINGLITERAL=(\"([^\"\r\n\\]|\\.)*\")|(\'([^\'\r\n\\]|\\.)*\')|unicode(\"([^\"])*\")|unicode(\'([^\"])*\')
DECIMALNUMBER=[0-9][_0-9]*
FIXEDNUMBER=(([0-9][_0-9]*)+\.[_0-9]*|([0-9][_0-9]*)*\.([0-9][_0-9]*))
SCIENTIFICNUMBER=((([0-9][_0-9]*)+|([0-9][_0-9]*)+\.[_0-9]*|([0-9][_0-9]*|[0-9])*\.([0-9][_0-9]*))[Ee][+-]?[_0-9]+)
HEXNUMBER=(0[xX][_0-9a-fA-F]+)
NUMBERUNIT=(wei|gwei|szabo|finney|ether|seconds|minutes|hours|days|weeks|years)
INTNUMTYPE=int(8|16|24|32|40|48|56|64|72|80|88|96|104|112|120|128|136|144|152|160|168|176|184|192|200|208|216|224|232|240|248|256)?
UINTNUMTYPE=uint(8|16|24|32|40|48|56|64|72|80|88|96|104|112|120|128|136|144|152|160|168|176|184|192|200|208|216|224|232|240|248|256)?
BYTENUMTYPE=byte(1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|23|24|25|26|27|28|29|30|31|32)?
BYTESNUMTYPE=bytes(1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|23|24|25|26|27|28|29|30|31|32)?
FIXEDNUMTYPE=fixed(0x8|0x16|0x24|0x32|0x40|0x48|0x56|0x64|0x72|0x80|0x88|0x96|0x104|0x112|0x120|0x128|0x136|0x144|0x152|0x160|0x168|0x176|0x184|0x192|0x200|0x208|0x216|0x224|0x232|0x240|0x248|0x256|8x8|8x16|8x24|8x32|8x40|8x48|8x56|8x64|8x72|8x80|8x88|8x96|8x104|8x112|8x120|8x128|8x136|8x144|8x152|8x160|8x168|8x176|8x184|8x192|8x200|8x208|8x216|8x224|8x232|8x240|8x248|16x8|16x16|16x24|16x32|16x40|16x48|16x56|16x64|16x72|16x80|16x88|16x96|16x104|16x112|16x120|16x128|16x136|16x144|16x152|16x160|16x168|16x176|16x184|16x192|16x200|16x208|16x216|16x224|16x232|16x240|24x8|24x16|24x24|24x32|24x40|24x48|24x56|24x64|24x72|24x80|24x88|24x96|24x104|24x112|24x120|24x128|24x136|24x144|24x152|24x160|24x168|24x176|24x184|24x192|24x200|24x208|24x216|24x224|24x232|32x8|32x16|32x24|32x32|32x40|32x48|32x56|32x64|32x72|32x80|32x88|32x96|32x104|32x112|32x120|32x128|32x136|32x144|32x152|32x160|32x168|32x176|32x184|32x192|32x200|32x208|32x216|32x224|40x8|40x16|40x24|40x32|40x40|40x48|40x56|40x64|40x72|40x80|40x88|40x96|40x104|40x112|40x120|40x128|40x136|40x144|40x152|40x160|40x168|40x176|40x184|40x192|40x200|40x208|40x216|48x8|48x16|48x24|48x32|48x40|48x48|48x56|48x64|48x72|48x80|48x88|48x96|48x104|48x112|48x120|48x128|48x136|48x144|48x152|48x160|48x168|48x176|48x184|48x192|48x200|48x208|56x8|56x16|56x24|56x32|56x40|56x48|56x56|56x64|56x72|56x80|56x88|56x96|56x104|56x112|56x120|56x128|56x136|56x144|56x152|56x160|56x168|56x176|56x184|56x192|56x200|64x8|64x16|64x24|64x32|64x40|64x48|64x56|64x64|64x72|64x80|64x88|64x96|64x104|64x112|64x120|64x128|64x136|64x144|64x152|64x160|64x168|64x176|64x184|64x192|72x8|72x16|72x24|72x32|72x40|72x48|72x56|72x64|72x72|72x80|72x88|72x96|72x104|72x112|72x120|72x128|72x136|72x144|72x152|72x160|72x168|72x176|72x184|80x8|80x16|80x24|80x32|80x40|80x48|80x56|80x64|80x72|80x80|80x88|80x96|80x104|80x112|80x120|80x128|80x136|80x144|80x152|80x160|80x168|80x176|88x8|88x16|88x24|88x32|88x40|88x48|88x56|88x64|88x72|88x80|88x88|88x96|88x104|88x112|88x120|88x128|88x136|88x144|88x152|88x160|88x168|96x8|96x16|96x24|96x32|96x40|96x48|96x56|96x64|96x72|96x80|96x88|96x96|96x104|96x112|96x120|96x128|96x136|96x144|96x152|96x160|104x8|104x16|104x24|104x32|104x40|104x48|104x56|104x64|104x72|104x80|104x88|104x96|104x104|104x112|104x120|104x128|104x136|104x144|104x152|112x8|112x16|112x24|112x32|112x40|112x48|112x56|112x64|112x72|112x80|112x88|112x96|112x104|112x112|112x120|112x128|112x136|112x144|120x8|120x16|120x24|120x32|120x40|120x48|120x56|120x64|120x72|120x80|120x88|120x96|120x104|120x112|120x120|120x128|120x136|128x8|128x16|128x24|128x32|128x40|128x48|128x56|128x64|128x72|128x80|128x88|128x96|128x104|128x112|128x120|128x128|136x8|136x16|136x24|136x32|136x40|136x48|136x56|136x64|136x72|136x80|136x88|136x96|136x104|136x112|136x120|144x8|144x16|144x24|144x32|144x40|144x48|144x56|144x64|144x72|144x80|144x88|144x96|144x104|144x112|152x8|152x16|152x24|152x32|152x40|152x48|152x56|152x64|152x72|152x80|152x88|152x96|152x104|160x8|160x16|160x24|160x32|160x40|160x48|160x56|160x64|160x72|160x80|160x88|160x96|168x8|168x16|168x24|168x32|168x40|168x48|168x56|168x64|168x72|168x80|168x88|176x8|176x16|176x24|176x32|176x40|176x48|176x56|176x64|176x72|176x80|184x8|184x16|184x24|184x32|184x40|184x48|184x56|184x64|184x72|192x8|192x16|192x24|192x32|192x40|192x48|192x56|192x64|200x8|200x16|200x24|200x32|200x40|200x48|200x56|208x8|208x16|208x24|208x32|208x40|208x48|216x8|216x16|216x24|216x32|216x40|224x8|224x16|224x24|224x32|232x8|232x16|232x24|240x8|240x16|248x8)?
UFIXEDNUMTYPE=ufixed(0x8|0x16|0x24|0x32|0x40|0x48|0x56|0x64|0x72|0x80|0x88|0x96|0x104|0x112|0x120|0x128|0x136|0x144|0x152|0x160|0x168|0x176|0x184|0x192|0x200|0x208|0x216|0x224|0x232|0x240|0x248|0x256|8x8|8x16|8x24|8x32|8x40|8x48|8x56|8x64|8x72|8x80|8x88|8x96|8x104|8x112|8x120|8x128|8x136|8x144|8x152|8x160|8x168|8x176|8x184|8x192|8x200|8x208|8x216|8x224|8x232|8x240|8x248|16x8|16x16|16x24|16x32|16x40|16x48|16x56|16x64|16x72|16x80|16x88|16x96|16x104|16x112|16x120|16x128|16x136|16x144|16x152|16x160|16x168|16x176|16x184|16x192|16x200|16x208|16x216|16x224|16x232|16x240|24x8|24x16|24x24|24x32|24x40|24x48|24x56|24x64|24x72|24x80|24x88|24x96|24x104|24x112|24x120|24x128|24x136|24x144|24x152|24x160|24x168|24x176|24x184|24x192|24x200|24x208|24x216|24x224|24x232|32x8|32x16|32x24|32x32|32x40|32x48|32x56|32x64|32x72|32x80|32x88|32x96|32x104|32x112|32x120|32x128|32x136|32x144|32x152|32x160|32x168|32x176|32x184|32x192|32x200|32x208|32x216|32x224|40x8|40x16|40x24|40x32|40x40|40x48|40x56|40x64|40x72|40x80|40x88|40x96|40x104|40x112|40x120|40x128|40x136|40x144|40x152|40x160|40x168|40x176|40x184|40x192|40x200|40x208|40x216|48x8|48x16|48x24|48x32|48x40|48x48|48x56|48x64|48x72|48x80|48x88|48x96|48x104|48x112|48x120|48x128|48x136|48x144|48x152|48x160|48x168|48x176|48x184|48x192|48x200|48x208|56x8|56x16|56x24|56x32|56x40|56x48|56x56|56x64|56x72|56x80|56x88|56x96|56x104|56x112|56x120|56x128|56x136|56x144|56x152|56x160|56x168|56x176|56x184|56x192|56x200|64x8|64x16|64x24|64x32|64x40|64x48|64x56|64x64|64x72|64x80|64x88|64x96|64x104|64x112|64x120|64x128|64x136|64x144|64x152|64x160|64x168|64x176|64x184|64x192|72x8|72x16|72x24|72x32|72x40|72x48|72x56|72x64|72x72|72x80|72x88|72x96|72x104|72x112|72x120|72x128|72x136|72x144|72x152|72x160|72x168|72x176|72x184|80x8|80x16|80x24|80x32|80x40|80x48|80x56|80x64|80x72|80x80|80x88|80x96|80x104|80x112|80x120|80x128|80x136|80x144|80x152|80x160|80x168|80x176|88x8|88x16|88x24|88x32|88x40|88x48|88x56|88x64|88x72|88x80|88x88|88x96|88x104|88x112|88x120|88x128|88x136|88x144|88x152|88x160|88x168|96x8|96x16|96x24|96x32|96x40|96x48|96x56|96x64|96x72|96x80|96x88|96x96|96x104|96x112|96x120|96x128|96x136|96x144|96x152|96x160|104x8|104x16|104x24|104x32|104x40|104x48|104x56|104x64|104x72|104x80|104x88|104x96|104x104|104x112|104x120|104x128|104x136|104x144|104x152|112x8|112x16|112x24|112x32|112x40|112x48|112x56|112x64|112x72|112x80|112x88|112x96|112x104|112x112|112x120|112x128|112x136|112x144|120x8|120x16|120x24|120x32|120x40|120x48|120x56|120x64|120x72|120x80|120x88|120x96|120x104|120x112|120x120|120x128|120x136|128x8|128x16|128x24|128x32|128x40|128x48|128x56|128x64|128x72|128x80|128x88|128x96|128x104|128x112|128x120|128x128|136x8|136x16|136x24|136x32|136x40|136x48|136x56|136x64|136x72|136x80|136x88|136x96|136x104|136x112|136x120|144x8|144x16|144x24|144x32|144x40|144x48|144x56|144x64|144x72|144x80|144x88|144x96|144x104|144x112|152x8|152x16|152x24|152x32|152x40|152x48|152x56|152x64|152x72|152x80|152x88|152x96|152x104|160x8|160x16|160x24|160x32|160x40|160x48|160x56|160x64|160x72|160x80|160x88|160x96|168x8|168x16|168x24|168x32|168x40|168x48|168x56|168x64|168x72|168x80|168x88|176x8|176x16|176x24|176x32|176x40|176x48|176x56|176x64|176x72|176x80|184x8|184x16|184x24|184x32|184x40|184x48|184x56|184x64|184x72|192x8|192x16|192x24|192x32|192x40|192x48|192x56|192x64|200x8|200x16|200x24|200x32|200x40|200x48|200x56|208x8|208x16|208x24|208x32|208x40|208x48|216x8|216x16|216x24|216x32|216x40|224x8|224x16|224x24|224x32|232x8|232x16|232x24|240x8|240x16|248x8)?
BOOLEANLITERAL=true|false
SPACE=[ \t\n\x0B\f\r]+
IDENTIFIER=[a-zA-Z_$][a-zA-Z_$0-9]*
// Pragma parses anything up to the trailing ';' to be fully forward-compatible.
PRAGMAALL=[^ ][^;]*

%%
<YYINITIAL> {
  {WHITE_SPACE}           { return WHITE_SPACE; }

  "."                     { return DOT; }
  ":"                     { return COLON; }
  ";"                     { return SEMICOLON; }
  ","                     { return COMMA; }
  "+"                     { return PLUS; }
  "-"                     { return MINUS; }
  "*"                     { return MULT; }
  "/"                     { return DIV; }
  "**"                    { return EXPONENT; }
  "!"                     { return NOT; }
  "="                     { return ASSIGN; }
  "=>"                    { return TO; }
  "=="                    { return EQ; }
  "!="                    { return NEQ; }
  "++"                    { return INC; }
  "--"                    { return DEC; }
  "+="                    { return PLUS_ASSIGN; }
  "-="                    { return MINUS_ASSIGN; }
  "*="                    { return MULT_ASSIGN; }
  "/="                    { return DIV_ASSIGN; }
  "|="                    { return OR_ASSIGN; }
  "^="                    { return XOR_ASSIGN; }
  "&="                    { return AND_ASSIGN; }
  "<<="                   { return LSHIFT_ASSIGN; }
  ">>="                   { return RSHIFT_ASSIGN; }
  "%="                    { return PERCENT_ASSIGN; }
  "<"                     { return LESS; }
  "<="                    { return LESSEQ; }
  ">"                     { return MORE; }
  ">="                    { return MOREEQ; }
  "^"                     { return CARET; }
  "&"                     { return AND; }
  "&&"                    { return ANDAND; }
  "|"                     { return OR; }
  "||"                    { return OROR; }
  "["                     { return LBRACKET; }
  "]"                     { return RBRACKET; }
  "{"                     { return LBRACE; }
  "}"                     { return RBRACE; }
  "("                     { return LPAREN; }
  ")"                     { return RPAREN; }
  "?"                     { return QUESTION; }
  "%"                     { return PERCENT; }
  "~"                     { return TILDE; }
  "<<"                    { return LSHIFT; }
  ">>"                    { return RSHIFT; }
  ":="                    { return LEFT_ASSEMBLY; }
  "=:"                    { return RIGHT_ASSEMBLY; }
  "/*"                    {
                            yybegin(IN_BLOCK_COMMENT);
                            yypushback(2);
                            zzBlockCommentStarted = zzCurrentPos;
                          }
  "//"                    {
                            yybegin(IN_EOL_COMMENT);
                            yypushback(2);
                          }
  "pragma"                {
                            yybegin(PRAGMA_OPEN);
                            return PRAGMA;
                          }
  "as"                    { return AS; }
  "import"                { return IMPORT; }
  "abstract"              { return ABSTRACT; }
  "contract"              { return CONTRACT; }
  "library"               { return LIBRARY; }
  "interface"             { return INTERFACE; }
  "is"                    { return IS; }
  "public"                { return PUBLIC; }
  "internal"              { return INTERNAL; }
  "private"               { return PRIVATE; }
  "constant"              { return CONSTANT; }
  "immutable"             { return IMMUTABLE; }
  "using"                 { return USING; }
  "for"                   { return FOR; }
  "struct"                { return STRUCT; }
  "modifier"              { return MODIFIER; }
  "virtual"               { return VIRTUAL; }
  "override"              { return OVERRIDE; }
  "external"              { return EXTERNAL; }
  "constructor"           { return CONSTRUCTOR; }
  "function"              { return FUNCTION; }
  "returns"               { return RETURNS; }
  "fallback"              { return FALLBACK; }
  "receive"               { return RECEIVE; }
  "event"                 { return EVENT; }
  "anonymous"             { return ANONYMOUS; }
  "enum"                  { return ENUM; }
  "type"                  { return TYPE; }
  "indexed"               { return INDEXED; }
  "var"                   { return VAR; }
  "mapping"               { return MAPPING; }
  "memory"                { return MEMORY; }
  "storage"               { return STORAGE; }
  "calldata"              { return CALLDATA; }
  "pure"                  { return PURE; }
  "view"                  { return VIEW; }
  "unchecked"             { return UNCHECKED; }
  "return"                { return RETURN; }
  "if"                    { return IF; }
  "else"                  { return ELSE; }
  "try"                   { return TRY; }
  "catch"                 { return CATCH; }
  "while"                 { return WHILE; }
  "assembly"              { return ASSEMBLY; }
  "do"                    { return DO; }
  "continue"              { return CONTINUE; }
  "break"                 { return BREAK; }
  "throw"                 { return THROW; }
  "emit"                  { return EMIT; }
  "delete"                { return DELETE; }
  "new"                   { return NEW; }
  "wei"                   { return WEI; }
  "gwei"                  { return GWEI; }
  "szabo"                 { return SZABO; }
  "finney"                { return FINNEY; }
  "ether"                 { return ETHER; }
  "seconds"               { return SECONDS; }
  "minutes"               { return MINUTES; }
  "hours"                 { return HOURS; }
  "days"                  { return DAYS; }
  "weeks"                 { return WEEKS; }
  "years"                 { return YEARS; }
  "address"               { return ADDRESS; }
  "string"                { return STRING; }
  "bool"                  { return BOOL; }

  {HEXLITERAL}            { return HEXLITERAL; }
  {STRINGLITERAL}         { return STRINGLITERAL; }
  {DECIMALNUMBER}         { return DECIMALNUMBER; }
  {FIXEDNUMBER}           { return FIXEDNUMBER; }
  {SCIENTIFICNUMBER}      { return SCIENTIFICNUMBER; }
  {HEXNUMBER}             { return HEXNUMBER; }
  {NUMBERUNIT}            { return NUMBERUNIT; }
  {INTNUMTYPE}            { return INTNUMTYPE; }
  {UINTNUMTYPE}           { return UINTNUMTYPE; }
  {BYTENUMTYPE}           { return BYTENUMTYPE; }
  {BYTESNUMTYPE}          { return BYTESNUMTYPE; }
  {FIXEDNUMTYPE}          { return FIXEDNUMTYPE; }
  {UFIXEDNUMTYPE}         { return UFIXEDNUMTYPE; }
  {BOOLEANLITERAL}        { return BOOLEANLITERAL; }
  {SPACE}                 { return SPACE; }
  {IDENTIFIER}            { return IDENTIFIER; }
}

// nested block comments are not supported, so don't track the occurrences of /*
<IN_BLOCK_COMMENT> {
  // to avoid tokenizing by whitespaces, and only split the comment into parts if
  // NatSpec tags are included.
  " @"                    {
                            yypushback(1);
                            return COMMENT;
                          }

  "*/"                    {
                            // do not treat /*/ as a comment
                            if (zzCurrentPos - zzBlockCommentStarted >= 2) {
                                yybegin(YYINITIAL);
                                return COMMENT;
                            }
                          }

  {NAT_SPEC_TAG}         {
                            //
                            if (yycharat(-1) == ' ') {
                                return NAT_SPEC_TAG;
                            }
                            return COMMENT;
                          }

  <<EOF>>                 { yybegin(YYINITIAL); }

  [^]                     { }
}

<IN_EOL_COMMENT> {
  // to avoid tokenizing by whitespaces, and only split the comment into parts if
  // NatSpec tags are included.
  " @"                    {
                            yypushback(1);
                            return COMMENT;
                          }

  {NAT_SPEC_TAG}          {
                              //
                              if (yycharat(-1) == ' ') {
                                  return NAT_SPEC_TAG;
                              }
                              return COMMENT;
                          }

  {EOL}                   {
                            yybegin(YYINITIAL);
                            // do not include '\n' in the comment
                            yypushback(1);
                            return COMMENT;
                          }

  [^]                     { }
}

<PRAGMA_OPEN> {
  {WHITE_SPACE}           { return WHITE_SPACE; }

  {IDENTIFIER}            {
                            yybegin(PRAGMA_REST);
                            return IDENTIFIER;
                          }

  <<EOF>>                 { yybegin(YYINITIAL); }
}

<PRAGMA_REST> {
  {WHITE_SPACE}           { return WHITE_SPACE; }

  {PRAGMAALL}             {
                            yybegin(YYINITIAL);
                            return PRAGMAALL;
                          }
  <<EOF>>                 { yybegin(YYINITIAL); }
}

[^] { return BAD_CHARACTER; }
