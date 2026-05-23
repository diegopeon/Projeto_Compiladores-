# Compilador Didatico em Java

Projeto academico de construcao de um **compilador completo**, desenvolvido em Java de forma incremental. Cada etapa corresponde a uma fase classica da teoria de compiladores, com codigo comentado e boas praticas de design aplicadas em todo o projeto.

---

## Indice

- [Sobre o Projeto](#sobre-o-projeto)
- [Linguagem Suportada](#linguagem-suportada)
- [Arquitetura](#arquitetura)
- [Etapas do Compilador](#etapas-do-compilador)
- [Como Executar](#como-executar)
- [Exemplos](#exemplos)
- [Estrutura de Pastas](#estrutura-de-pastas)
- [Criterios de Avaliacao](#criterios-de-avaliacao)

---

## Sobre o Projeto

Este compilador foi desenvolvido como trabalho da disciplina de **Teoria de Linguagens Formais e Automatos**. O objetivo e consolidar os conceitos teoricos — automatos finitos, gramaticas livres de contexto, tabelas de simbolos e geracao de codigo — por meio de uma implementacao pratica e funcional.

O pipeline e composto por **5 etapas**, executadas em sequencia. O Main.java demonstra todas elas de forma visual e humanizada, exibindo para cada programa:

```
[1] Codigo-fonte original
[2] Tokens gerados        (Etapa 1 - Lexico)
[3] AST construida        (Etapa 2 - Sintatico)
[4] Validacao semantica   (Etapa 3 - Semantico)
[5] Codigo TAC gerado     (Etapa 4 - IR)
[6] Assembly x86-64       (Etapa 5a - Codegen)
[7] Execucao na VM        (Etapa 5b - Maquina Virtual)
```

---

## Linguagem Suportada

A linguagem-alvo e uma linguagem imperativa simples, criada para fins didaticos:

| Recurso | Exemplos |
|---|---|
| Tipos de dados | `int`, `bool`, `string` |
| Controle de fluxo | `if`, `else`, `while` |
| Operadores aritmeticos | `+`, `-`, `*`, `/` |
| Operadores relacionais | `==`, `!=`, `<`, `>`, `<=`, `>=` |
| Operadores logicos | `&&`, `||`, `!` |
| Entrada/Saida | `print(...)`, `read(...)` |
| Comentarios | `// linha` e `/* bloco */` |

### Exemplo de programa valido

```
int soma = 0;
int i = 1;
while (i <= 10) {
    soma = soma + i;
    i = i + 1;
}
print(soma); // Saida: 55
```

---

## Arquitetura

```
Codigo-Fonte
     |
     v
+-------------------+
|  Etapa 1 - Lexico |  Tokens
+-------------------+
     |
     v
+----------------------+
|  Etapa 2 - Sintatico |  AST (Arvore de Sintaxe Abstrata)
+----------------------+
     |
     v
+-----------------------+
|  Etapa 3 - Semantico  |  Tabela de Simbolos + Verificacao de Tipos
+-----------------------+
     |
     v
+--------------------+
|  Etapa 4 - IR/TAC  |  Codigo de Tres Enderecos
+--------------------+
     |
     v
+---------------------+     +------------------+
|  Etapa 5a - Assembly | --> | Codigo x86-64    |
+---------------------+     +------------------+
     |
     v
+---------------------+     +------------------+
|  Etapa 5b - VM       | --> | Execucao Real    |
+---------------------+     +------------------+
```

---

## Etapas do Compilador

### Etapa 1 — Analise Lexica (Scanner)
**Status: Concluida**

Transforma o fluxo de caracteres em uma sequencia de **tokens**.

- Reconhece palavras reservadas, identificadores, literais, operadores e delimitadores
- Ignora espacos em branco e comentarios `//` e `/* */`
- Reporta erros lexicos com linha e coluna precisas
- **Teoria aplicada:** Expressoes Regulares e Automatos Finitos Deterministicos (AFD)

| Arquivo | Responsabilidade |
|---|---|
| `TokenType.java` | Enum com todos os tipos de token da linguagem |
| `Token.java` | Value Object imutavel: tipo + lexema + posicao |
| `Lexer.java` | Scanner — maquina de estados que produz a lista de tokens |

---

### Etapa 2 — Analise Sintatica (Parser)
**Status: Concluida**

Agrupa os tokens em estruturas gramaticais e constroi a **AST**.

- Parser Descendente Recursivo
- Hierarquia de chamadas garante a **precedencia correta** de operadores
- Erros sintaticos com linha e coluna exatas
- **Teoria aplicada:** Gramaticas Livres de Contexto (GLC)

Gramatica implementada:

```
program    -> statement* EOF
statement  -> varDecl | assign | ifStmt | whileStmt | print | read
expression -> logicalOr
logicalOr  -> logicalAnd  ('||' logicalAnd)*
logicalAnd -> equality    ('&&' equality)*
equality   -> relational  ('=='|'!=' relational)*
relational -> additive    ('<'|'>'|'<='|'>=' additive)*
additive   -> multip      ('+'|'-' multip)*
multip     -> unary       ('*'|'/' unary)*
unary      -> ('!'|'-') unary | primary
primary    -> literal | identifier | '(' expression ')'
```

| Arquivo | Responsabilidade |
|---|---|
| `Node.java` | Interface base de todos os nos da AST |
| `NodeVisitor.java` | Visitor Pattern — usado em todas as fases seguintes |
| `stmt/` (7 classes) | `ProgramNode`, `BlockStmt`, `VarDeclStmt`, `AssignStmt`, `IfStmt`, `WhileStmt`, `PrintStmt`, `ReadStmt` |
| `expr/` (6 classes) | `BinaryExpr`, `UnaryExpr`, `IdentifierExpr`, `IntLiteralExpr`, `BoolLiteralExpr`, `StringLiteralExpr` |
| `Parser.java` | Parser descendente recursivo |
| `AstPrinter.java` | Visitor de debug — imprime a AST como arvore indentada |
| `ParseException.java` | Excecao de erro sintatico |

---

### Etapa 3 — Analise Semantica
**Status: Concluida**

Garante que o programa faca **sentido logicamente**.

- Tabela de Simbolos com pilha de escopos (cada `{ }` cria e destroi seu escopo)
- Verificacao de tipos em declaracoes, atribuicoes e expressoes
- **Teoria aplicada:** Gramaticas com Atributos

Erros detectados:

| Erro | Exemplo |
|---|---|
| Variavel nao declarada | `print(y)` sem declarar `y` |
| Variavel declarada duas vezes | `int x = 1; int x = 2;` |
| Tipo incompativel na declaracao | `int x = true;` |
| Tipo incompativel na atribuicao | `x = false;` sendo `x` um `int` |
| Operacao aritmetica com bool | `b + 1` sendo `b` um `bool` |
| Condicao nao booleana | `if (42) { ... }` |
| Variavel usada fora do escopo | declarar `y` no `if` e usar fora |

| Arquivo | Responsabilidade |
|---|---|
| `DataType.java` | Enum `INT`, `BOOL`, `STRING`, `VOID` |
| `Symbol.java` | Entrada da tabela: nome + tipo + linha de declaracao |
| `SymbolTable.java` | Pilha de escopos com `pushScope()` / `popScope()` |
| `SemanticAnalyzer.java` | Visitor que valida tipos e escopos na AST |
| `SemanticException.java` | Excecao de erro semantico com posicao |

---

### Etapa 4 — Geracao de Codigo Intermediario (IR/TAC)
**Status: Concluida**

Traduz a AST para **Codigo de Tres Enderecos (TAC)** — representacao linear, independente de maquina.

Instrucoes geradas:

| Instrucao | Exemplo | Descricao |
|---|---|---|
| Copia | `x = 42` | Atribuicao simples |
| Binaria | `t0 = x + y` | Operacao com dois operandos |
| Unaria | `t1 = !ativo` | Negacao logica ou aritmetica |
| Desvio condicional | `ifFalse t0 goto L1` | Sai do bloco se falso |
| Desvio incondicional | `goto L0` | Volta ao topo do while |
| Label | `L0:` | Marcador de posicao |
| Print / Read | `print soma` / `read n` | Entrada e saida |

Exemplo — `while (i <= 5) { soma = soma + i; }`:

```
L0:
    t0 = i <= 5
    ifFalse t0 goto L1
    t1 = soma + i
    soma = t1
    goto L0
L1:
```

| Arquivo | Responsabilidade |
|---|---|
| `TacInstruction.java` | Representa uma instrucao TAC com tipo e operandos |
| `IrProgram.java` | Lista ordenada de instrucoes — resultado da etapa |
| `IrGenerator.java` | Visitor que percorre a AST e emite as instrucoes TAC |

---

### Etapa 5 — Geracao de Codigo Final
**Status: Concluida**

Dois sub-modulos independentes foram implementados:

#### 5a — Assembly x86-64 (AT&T syntax)

Traduz o TAC para Assembly x86-64, pronto para ser montado com `gcc`:

```asm
.section .data
fmt_int: .string "%ld\n"

.section .text
.global main
main:
    pushq %rbp
    movq  %rsp, %rbp
    subq  $48, %rsp

    # soma = 0
    movq  $0, %rax
    movq  %rax, -8(%rbp)

    # i = 1
    movq  $1, %rax
    movq  %rax, -16(%rbp)
L0:
    # t0 = i <= 10
    movq  -16(%rbp), %rax
    movq  %rax, %rbx
    movq  $10, %rax
    cmpq  %rax, %rbx
    setle %al
    movzbq %al, %rax
    movq  %rax, -24(%rbp)

    # ifFalse t0 goto L1
    movq  -24(%rbp), %rax
    cmpq  $0, %rax
    je    L1
    ...
```

#### 5b — Maquina Virtual (VM)

Interpreta o TAC diretamente em Java, permitindo executar e ver o resultado sem precisar de um montador externo.

- Memoria: mapa de nome -> valor (substitui pilha e registradores)
- Program Counter: percorre as instrucoes em ordem
- Suporte a saltos, lacos, operacoes aritmeticas/logicas e E/S
- Protecao contra loop infinito (limite de 100.000 instrucoes)

| Arquivo | Responsabilidade |
|---|---|
| `AssemblyGenerator.java` | Traduz TAC para Assembly x86-64 AT&T |
| `VirtualMachine.java` | Interpreta e executa o TAC diretamente |

---

## Como Executar

### Pre-requisitos

- Java 17 ou superior
- IDE recomendada: Eclipse ou IntelliJ IDEA

### Compilar pelo terminal

```bash
mkdir -p out
find src -name "*.java" | xargs javac -d out
java -cp out compiler.Main
```

### Importar no Eclipse

1. Extraia o `.zip` do projeto
2. `File -> New -> Java Project` -> nome `compilador`
3. Botao direito no projeto -> `Build Path -> Configure Build Path`
4. Aba **Source** -> adicione a pasta `src` extraida
5. Botao direito em `Main.java` -> `Run As -> Java Application`

---

## Exemplos

### Soma de 1 a 10 — saida completa do pipeline

**Codigo-fonte:**
```
int soma = 0;
int i = 1;
while (i <= 10) {
    soma = soma + i;
    i = i + 1;
}
print(soma);
```

**TAC gerado:**
```
    soma = 0
    i = 1
L0:
    t0 = i <= 10
    ifFalse t0 goto L1
    t1 = soma + i
    soma = t1
    t2 = i + 1
    i = t2
    goto L0
L1:
    print soma
```

**Saida da VM:**
```
>>> 55
```

---

### Erro semantico detectado

```
int x = 5;
if (x) {        // ERRO: condicao deve ser bool, encontrado int
    print("ok");
}
```

```
[ERRO] Erro semantico [linha 2]: condicao do 'if' deve ser do tipo 'bool', encontrado 'int'
```

---

## Estrutura de Pastas

```
compiler/
├── src/
│   └── compiler/
│       ├── Main.java                    <- ponto de entrada (pipeline completo)
│       │
│       ├── lexer/                       <- Etapa 1
│       │   ├── TokenType.java
│       │   ├── Token.java
│       │   └── Lexer.java
│       │
│       ├── ast/                         <- Etapa 2
│       │   ├── Node.java
│       │   ├── NodeVisitor.java
│       │   ├── stmt/                    (7 classes)
│       │   └── expr/                    (6 classes)
│       │
│       ├── parser/                      <- Etapa 2
│       │   ├── Parser.java
│       │   ├── AstPrinter.java
│       │   └── ParseException.java
│       │
│       ├── semantic/                    <- Etapa 3
│       │   ├── DataType.java
│       │   ├── Symbol.java
│       │   ├── SymbolTable.java
│       │   ├── SemanticAnalyzer.java
│       │   └── SemanticException.java
│       │
│       ├── ir/                          <- Etapa 4
│       │   ├── TacInstruction.java
│       │   ├── IrProgram.java
│       │   └── IrGenerator.java
│       │
│       └── codegen/                     <- Etapa 5
│           ├── AssemblyGenerator.java
│           └── VirtualMachine.java
│

```

**Total: 30 arquivos .java — 5 etapas completas**

---

## Criterios de Avaliacao

| Criterio | Peso | Status |
|---|---|---|
| Corretude Lexica/Sintatica | 30% | Concluido |
| Analise Semantica | 20% | Concluido |
| Geracao de Codigo | 30% | Concluido (TAC + Assembly + VM) |
| Documentacao / Codigo | 20% | Concluido |

---

## Boas Praticas Aplicadas

- **Responsabilidade Unica (SRP):** cada classe tem uma funcao bem definida
- **Visitor Pattern:** `NodeVisitor<T>` desacopla as fases do compilador dos nos da AST — adicionar uma nova fase nao exige modificar nenhum no existente
- **Imutabilidade:** `Token` e `Symbol` sao Value Objects sem estado mutavel
- **Coleta de erros:** cada fase reporta erros com linha, coluna e contexto claros
- **Comentarios de intencao:** explicam o *porque* das decisoes, nao apenas o *o que*
- **Nomenclatura descritiva:** metodos e variaveis com nomes que explicam seu papel
