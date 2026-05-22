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

Este compilador foi desenvolvido como trabalho da disciplina de **Teoria de Linguagens Formais e Autômatos**. O objetivo é consolidar os conceitos teóricos:  autômatos finitos, gramáticas livres de contexto, tabelas de símbolos e geração de código.
Por meio de uma implementação prática e funcional.

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
│  Analisador │  Etapa 1 — Léxico
│    Léxico   │  Tokens
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Analisador │  Etapa 2 — Sintático
│  Sintático  │  AST (Árvore de Sintaxe Abstrata)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Analisador │  Etapa 3 — Semântico
│  Semântico  │  Tabela de Símbolos + Verificação de Tipos
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Gerador de │  Etapa 4 — Código Intermediário
│  Código IR  │  TAC (Three-Address Code)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Gerador de │  Etapa 5 — Código Final
│ Código Final│  Assembly / Bytecode
└─────────────┘
```

---

## Etapas do Compilador

### ✅ Etapa 1 — Análise Léxica (Scanner)
**Status: Concluída**

Transforma o fluxo de caracteres em uma sequência de **tokens**.

- Reconhece: palavras reservadas, identificadores, literais (inteiros, booleanos, strings), operadores e delimitadores
- Ignora: espaços em branco, comentários de linha (`//`) e bloco (`/* */`)
- Reporta: erros léxicos com linha e coluna precisas, coletando todos antes de abortar
- **Teoria aplicada:** Expressões Regulares e Autômatos Finitos Determinísticos (AFD)

**Módulos:**
| Arquivo | Responsabilidade |
|---|---|
| `TokenType.java` | Enum com todos os tipos de token da linguagem |
| `Token.java` | Value Object imutável representando um token (tipo + lexema + posição) |
| `Lexer.java` | Scanner principal — máquina de estados que produz a lista de tokens |

---

### 🔜 Etapa 2 — Análise Sintática (Parser)
**Status: Em desenvolvimento**

Agrupa os tokens em estruturas gramaticais e constrói a **AST**.

- Método: Parser Descendente Recursivo
- **Teoria aplicada:** Gramáticas Livres de Contexto (GLC)

---

### 🔜 Etapa 3 — Análise Semântica
**Status: Pendente**

Garante que o programa faça sentido logicamente.

- Tabela de Símbolos (escopo e declaração prévia de variáveis)
- Verificação de tipos (*type checking*)
- **Teoria aplicada:** Gramáticas com Atributos

---

### 🔜 Etapa 4 — Geração de Código Intermediário
**Status: Pendente**

Traduz a AST para uma representação independente de máquina.

- Código de Três Endereços (TAC)
- **Teoria aplicada:** Esquemas de Tradução Dirigida pela Sintaxe (SDT)

---

### 🔜 Etapa 5 — Geração de Código Final
**Status: Pendente**

Traduz o código intermediário para linguagem de baixo nível.

- Assembly x86 ou Bytecode para máquina virtual
- Otimizações: eliminação de código morto, simplificação de expressões

---

## Como Executar

### Pré-requisitos

- Java 17 ou superior ([download](https://adoptium.net/))

### Compilar

```bash
# Na raiz do projeto
mkdir -p out
find src -name "*.java" | xargs javac -d out
```

### Executar os testes

```bash
java -cp out compiler.Main
```

### Saída esperada (Etapa 1)

```
=== Compilador — Teste da Etapa 1: Análise Léxica ===

┌─────────────────────────────────────────
│ TESTE: Declaração e atribuição simples
├─────────────────────────────────────────
│ Código-fonte:
│   1 │ int x = 42;
├─────────────────────────────────────────
│ Tokens produzidos:
│   KW_INT               "int"           linha 1   col 1
│   IDENTIFIER           "x"             linha 1   col 5
│   OP_ASSIGN            "="             linha 1   col 7
│   INTEGER_LITERAL      "42"            linha 1   col 9
│   SEMICOLON            ";"             linha 1   col 11
│ ✓  Nenhum erro léxico encontrado.
└─────────────────────────────────────────
```

---

## Estrutura de Pastas

```
compiler/src/compiler/
│
├── Main.java                        ← ponto de entrada e testes
│
├── lexer/                           ✅ ETAPA 1 — concluída
│   ├── TokenType.java               · enum com todos os tipos de token
│   ├── Token.java                   · value object (tipo + lexema + posição)
│   └── Lexer.java                   · scanner / máquina de estados
│
├── ast/                             ✅ ETAPA 2 — concluída
│   ├── Node.java                    · interface base de todos os nós
│   ├── NodeVisitor.java             · Visitor Pattern (usado em todas as fases)
│   │
│   ├── stmt/                        · instruções (não retornam valor)
│   │   ├── Stmt.java                · classe base abstrata
│   │   ├── ProgramNode.java         · raiz da AST
│   │   ├── BlockStmt.java           · bloco { ... }
│   │   ├── VarDeclStmt.java         · int x = 42;
│   │   ├── AssignStmt.java          · x = expr;
│   │   ├── IfStmt.java              · if/else
│   │   ├── WhileStmt.java           · while
│   │   ├── PrintStmt.java           · print(expr);
│   │   └── ReadStmt.java            · read(var);
│   │
│   └── expr/                        · expressões (retornam um valor)
│       ├── Expr.java                · classe base abstrata
│       ├── BinaryExpr.java          · left OP right
│       ├── UnaryExpr.java           · ! expr  /  - expr
│       ├── IdentifierExpr.java      · nome de variável
│       ├── IntLiteralExpr.java      · 42
│       ├── BoolLiteralExpr.java     · true / false
│       └── StringLiteralExpr.java   · "texto"
│
├── parser/                          ✅ ETAPA 2 — concluída
│   ├── Parser.java                  · parser descendente recursivo
│   ├── AstPrinter.java              · visitor que imprime a AST
│   └── ParseException.java          · erro sintático com posição
│
├── semantic/                        ⏳ ETAPA 3 — próxima
│   ├── SemanticAnalyzer.java        · visitor que percorre a AST
│   ├── SymbolTable.java             · controle de escopo e tipos
│   └── SemanticException.java       · erro semântico
│
├── ir/                              ⏳ ETAPA 4
│   └── IrGenerator.java             · gera código de 3 endereços (TAC)
│
└── codegen/                         ⏳ ETAPA 5
    └── CodeGenerator.java           · traduz TAC para Assembly  
```

---

## Critérios de Avaliação

| Critério | Peso | Status |
|---|---|---|
| Corretude Léxica/Sintática | 30% | ✅ Em progresso |
| Análise Semântica | 20% | ⏳ Pendente |
| Geração de Código | 30% | ⏳ Pendente |
| Documentação / Código | 20% | ✅ Aplicado desde o início |

---

## Boas Práticas Aplicadas

- **Responsabilidade Única (SRP):** cada classe tem uma função bem definida
- **Imutabilidade:** `Token` é um Value Object — sem setters, sem estado mutável
- **Coleta de erros:** o compilador acumula todos os erros léxicos antes de abortar, como compiladores reais fazem
- **Comentários de intenção:** explicam o *porquê* das decisões, não apenas o *o quê*
- **Nomenclatura descritiva:** métodos e variáveis com nomes que explicam seu papel
