# 🛠️ Compilador Didático em Java

Projeto acadêmico de construção de um **compilador completo**, desenvolvido em Java de forma incremental. Cada etapa corresponde a uma fase clássica da teoria de compiladores, com código comentado e boas práticas de design aplicadas em todo o projeto.

---

## 📋 Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Linguagem Suportada](#linguagem-suportada)
- [Arquitetura](#arquitetura)
- [Etapas do Compilador](#etapas-do-compilador)
- [Como Executar](#como-executar)
- [Exemplos de Código-Fonte](#exemplos-de-código-fonte)
- [Estrutura de Pastas](#estrutura-de-pastas)
- [Critérios de Avaliação](#critérios-de-avaliação)

---

## Sobre o Projeto

Este compilador foi desenvolvido como trabalho da disciplina de **Teoria de Linguagens Formais e Autômatos**. O objetivo é consolidar os conceitos teóricos: autômatos finitos, gramáticas livres de contexto, tabelas de símbolos e geração de código, por meio de uma implementação prática e funcional.

O desenvolvimento é **metódico e incremental**: cada fase é implementada, testada e documentada antes de avançar para a próxima.

---

## Linguagem Suportada

A linguagem-alvo é uma linguagem imperativa simples, criada para fins didáticos:

| Recurso | Exemplos |
|---|---|
| **Tipos de dados** | `int`, `bool`, `string` |
| **Controle de fluxo** | `if`, `else`, `while` |
| **Operadores aritméticos** | `+`, `-`, `*`, `/` |
| **Operadores relacionais** | `==`, `!=`, `<`, `>`, `<=`, `>=` |
| **Operadores lógicos** | `&&`, `\|\|`, `!` |
| **Entrada/Saída** | `print(...)`, `read(...)` |
| **Comentários** | `// linha` e `/* bloco */` |

### Exemplo de programa válido

```
int soma = 0;
int i = 1;

while (i <= 10) {
    soma = soma + i;
    i = i + 1;
}

print(soma); // Saída: 55
```

---

## Arquitetura

O compilador segue o **pipeline clássico de tradução**:

```
Código-Fonte
     │
     ▼
┌─────────────┐
│  Analisador │  Etapa 1 — Léxico       ✅ Concluída
│    Léxico   │  → Lista de Tokens
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Analisador │  Etapa 2 — Sintático    ✅ Concluída
│  Sintático  │  → AST (Árvore de Sintaxe Abstrata)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Analisador │  Etapa 3 — Semântico    ✅ Concluída
│  Semântico  │  → Tabela de Símbolos + Verificação de Tipos
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Gerador de │  Etapa 4 — Código IR    ⏳ Em breve
│  Código IR  │  → TAC (Three-Address Code)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Gerador de │  Etapa 5 — Código Final ⏳ Em breve
│ Código Final│  → Assembly / Bytecode
└─────────────┘
```

---

## Etapas do Compilador

### ✅ Etapa 1 — Análise Léxica (Scanner)
**Status: Concluída**

Transforma o fluxo de caracteres em uma sequência de **tokens**.

- Reconhece palavras reservadas, identificadores, literais (inteiros, booleanos, strings), operadores e delimitadores
- Ignora espaços em branco e comentários de linha (`//`) e bloco (`/* */`)
- Reporta erros léxicos com linha e coluna precisas, coletando todos antes de abortar
- **Teoria aplicada:** Expressões Regulares e Autômatos Finitos Determinísticos (AFD)

| Arquivo | Responsabilidade |
|---|---|
| `TokenType.java` | Enum com todos os tipos de token da linguagem |
| `Token.java` | Value Object imutável: tipo + lexema + posição |
| `Lexer.java` | Scanner — máquina de estados que produz a lista de tokens |

---

### ✅ Etapa 2 — Análise Sintática (Parser)
**Status: Concluída**

Agrupa os tokens em estruturas gramaticais e constrói a **AST (Árvore de Sintaxe Abstrata)**.

- Parser Descendente Recursivo
- Hierarquia de chamadas garante a **precedência correta** de operadores (`*` antes de `+`, etc.)
- Erros sintáticos com linha e coluna exatas
- **Teoria aplicada:** Gramáticas Livres de Contexto (GLC)

| Arquivo | Responsabilidade |
|---|---|
| `Node.java` | Interface base de todos os nós da AST |
| `NodeVisitor.java` | Visitor Pattern — usado em todas as fases seguintes |
| `stmt/` (7 classes) | Nós de instrução: `ProgramNode`, `BlockStmt`, `VarDeclStmt`, `AssignStmt`, `IfStmt`, `WhileStmt`, `PrintStmt`, `ReadStmt` |
| `expr/` (6 classes) | Nós de expressão: `BinaryExpr`, `UnaryExpr`, `IdentifierExpr`, `IntLiteralExpr`, `BoolLiteralExpr`, `StringLiteralExpr` |
| `Parser.java` | Parser descendente recursivo |
| `AstPrinter.java` | Visitor de debug — imprime a AST como árvore indentada |
| `ParseException.java` | Exceção de erro sintático |

---

### ✅ Etapa 3 — Análise Semântica
**Status: Concluída**

Garante que o programa faça **sentido logicamente**, além de ser sintaticamente correto.

- **Tabela de Símbolos** com pilha de escopos — cada bloco `{ }` cria e destrói seu próprio escopo
- **Verificação de tipos** em declarações, atribuições e expressões
- **Teoria aplicada:** Gramáticas com Atributos e Esquemas de Tradução

Erros detectados:

| Erro | Exemplo |
|---|---|
| Variável não declarada | `print(y)` sem declarar `y` |
| Variável declarada duas vezes | `int x = 1; int x = 2;` |
| Tipo incompatível na declaração | `int x = true;` |
| Tipo incompatível na atribuição | `x = false;` sendo `x` um `int` |
| Operação aritmética com bool | `b + 1` sendo `b` um `bool` |
| Condição não booleana | `if (42) { ... }` |
| Variável usada fora do escopo | declarar `y` dentro do `if` e usar fora |

| Arquivo | Responsabilidade |
|---|---|
| `DataType.java` | Enum `INT`, `BOOL`, `STRING`, `VOID` |
| `Symbol.java` | Entrada da tabela: nome + tipo + linha de declaração |
| `SymbolTable.java` | Pilha de escopos com `pushScope()` / `popScope()` |
| `SemanticAnalyzer.java` | Visitor que percorre a AST e valida tipos e escopos |
| `SemanticException.java` | Exceção de erro semântico com posição |

---

### ⏳ Etapa 4 — Geração de Código Intermediário
**Status: Pendente**

Traduz a AST para uma representação independente de máquina.

- Código de Três Endereços (TAC)
- Variáveis temporárias (`t0`, `t1`, …) e labels de desvio
- **Teoria aplicada:** Esquemas de Tradução Dirigida pela Sintaxe (SDT)

---

### ⏳ Etapa 5 — Geração de Código Final
**Status: Pendente**

Traduz o código intermediário para linguagem de baixo nível.

- Assembly x86 ou Bytecode para máquina virtual
- Otimizações (bônus): eliminação de código morto, simplificação de expressões

---

## Como Executar

### Pré-requisitos

- Java 17 ou superior ([download](https://adoptium.net/))
- IDE recomendada: Eclipse ou IntelliJ IDEA

### Compilar pelo terminal

```bash
# Na raiz do projeto
mkdir -p out
find src -name "*.java" | xargs javac -d out
```

### Executar os testes

```bash
java -cp out compiler.Main
```

### Importar no Eclipse

1. Extraia o `.zip` do projeto
2. `File → New → Java Project` → dê o nome `compilador`
3. Botão direito no projeto → `Build Path → Configure Build Path`
4. Aba **Source** → adicione a pasta `src` extraída
5. Clique com botão direito em `Main.java` → `Run As → Java Application`

---

## Exemplos de Código-Fonte

### Programa válido — soma de 1 a 10

```
int soma = 0;
int i = 1;
while (i <= 10) {
    soma = soma + i;
    i = i + 1;
}
print(soma);
```

### Programa com erro semântico

```
int x = 10;
if (x) {          // ERRO: condição deve ser bool, encontrado int
    print("ok");
}
```

### Programa com escopo aninhado

```
int x = 5;
if (x > 0) {
    int y = x + 1;  // y existe só dentro deste bloco
    print(y);
}
print(y);           // ERRO: variável 'y' não foi declarada
```

---

## Estrutura de Pastas

```
compiler/
├── src/
│   └── compiler/
│       ├── Main.java                    ← ponto de entrada e testes
│       │
│       ├── lexer/                       ← Etapa 1 ✅
│       │   ├── TokenType.java
│       │   ├── Token.java
│       │   └── Lexer.java
│       │
│       ├── ast/                         ← Etapa 2 ✅
│       │   ├── Node.java
│       │   ├── NodeVisitor.java
│       │   ├── stmt/                    (7 classes)
│       │   └── expr/                    (6 classes)
│       │
│       ├── parser/                      ← Etapa 2 ✅
│       │   ├── Parser.java
│       │   ├── AstPrinter.java
│       │   └── ParseException.java
│       │
│       ├── semantic/                    ← Etapa 3 ✅
│       │   ├── DataType.java
│       │   ├── Symbol.java
│       │   ├── SymbolTable.java
│       │   ├── SemanticAnalyzer.java
│       │   └── SemanticException.java
│       │
│       ├── ir/                          ← Etapa 4 ⏳
│       │   └── IrGenerator.java
│       │
│       └── codegen/                     ← Etapa 5 ⏳
│           └── CodeGenerator.java
│
└── out/                                 ← bytecode compilado (gerado)
```

---

## Critérios de Avaliação

| Critério | Peso | Status |
|---|---|---|
| Corretude Léxica/Sintática | 30% | ✅ Concluído |
| Análise Semântica | 20% | ✅ Concluído |
| Geração de Código | 30% | ⏳ Pendente |
| Documentação / Código | 20% | ✅ Aplicado desde o início |

---

## Boas Práticas Aplicadas

- **Responsabilidade Única (SRP):** cada classe tem uma função bem definida
- **Visitor Pattern:** `NodeVisitor<T>` desacopla as fases do compilador dos nós da AST — adicionar uma nova fase não exige modificar nenhum nó existente
- **Imutabilidade:** `Token` e `Symbol` são Value Objects — sem setters, sem estado mutável
- **Coleta de erros:** o lexer acumula todos os erros antes de abortar; o semântico reporta o erro com linha e contexto claros
- **Comentários de intenção:** explicam o *porquê* das decisões, não apenas o *o quê*
- **Nomenclatura descritiva:** métodos e variáveis com nomes que explicam seu papel
