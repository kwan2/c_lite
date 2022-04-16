# c_lite

## 프로그래밍언어개념 수업 중 진행한 과제 이다. 

C_lite 언어를 java를 이용하여 구현

정규 언어에 사용하며 Type 3 문법 (RG) 에 속하며 구문 분석 (Parsing) 하는 과정이다. 

1. Token 화 - Token.java, TokenType.java,
2. Lexer - Lexer.java,  
3. AST 구성 - AbstractSyntax.java , Parser.java  
4. Type Parsing -  TypeMap.java , StaticTypeCheck.java(정적) , TypeTransformer.java (동적) 형변환 
6. 함수 모듈 화 - Semantics.java, State.java (call chain, state 전이 )
