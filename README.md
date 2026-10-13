# Sistema de Folha de Pagamento

Sistema de folha de pagamento em Java (console), com banco de dados SQLite, desenvolvido para o projeto final da disciplina de Garantia da Qualidade de Software (Una Contagem).

## Problema

Muitas pequenas empresas calculam a folha de pagamento à mão ou em planilhas, o que gera erros nos descontos de INSS e IRRF, benefícios descontados acima do permitido e falta de histórico confiável.

## Solução

Um sistema que cadastra empresas e funcionários, calcula a folha automaticamente com as regras de 2026, controla benefícios com limites de desconto e guarda o histórico de todas as folhas.

## ODS

ODS 8 - Trabalho Decente e Crescimento Econômico.

## Funcionalidades

- Login da empresa por CNPJ e senha (hash SHA-256 com salt), com 3 tentativas
- Cadastro de empresa e redefinição de senha
- Cadastro e edição de funcionários (cargo e salário base)
- Cálculo da folha: INSS progressivo, IRRF 2026 e FGTS
- Benefícios com desconto em % e limites:
  - Vale-transporte: até 6% do salário
  - Vale-refeição e vale-alimentação: até 20% do valor do benefício
  - Demais benefícios: até 10% do salário (regra do projeto)
- Editar e remover benefícios de um funcionário
- Folha lançada como PENDENTE, com opção de confirmar ou apagar (só pendentes)
- Histórico de folhas por funcionário e por mês da empresa, com totais

## Tecnologias

- Java 21
- SQLite com JDBC (sqlite-jdbc)
- Eclipse IDE
- Git e GitHub (Git Flow)

## Como executar

1. Clone o repositório e importe no Eclipse (File > Import > Existing Projects into Workspace).
2. Adicione o jar do sqlite-jdbc ao Build Path do projeto.
3. Crie o banco `database/folha.db` executando o script `database/folha2.db.sql` (por exemplo, no DB Browser for SQLite). O arquivo `folha.db` não vai para o Git.
4. Execute a classe `Folha_Pagamentos.folha`.

## Versionamento

Git Flow: `main` (versões entregues), `develop` (integração) e branches `feature/...` para cada funcionalidade, sempre integradas por Pull Request. Commits semânticos (`feat:`, `fix:`, `docs:`).

Professor: Daniel Henrique Matos de Paiva
