# Collection da API

`valoriza-mei.postman_collection.json` reúne as 40 requisições da API no formato Postman Collection v2.1, que também é importado pelo Bruno.

## Importar

- **Postman:** *Import* → selecione o arquivo `valoriza-mei.postman_collection.json`.
- **Bruno:** *Import Collection* → *Postman Collection* → selecione o mesmo arquivo e escolha uma pasta para salvar a collection.

## Usar

1. Suba a API (`.\mvnw.cmd spring-boot:test-run`, ver o README do projeto).
2. Rode **1. Autenticação → Cadastro de conta** e **Login**. O login guarda o token na variável `token`, usada em todas as requisições protegidas (Bearer, definido na collection).
3. Siga as pastas em ordem. Os cadastros guardam os ids em `produtoId`, `despesaId`, `movimentacaoId` e `dataComemorativaId`, usados nas consultas seguintes.

A collection também pode ser executada inteira, em ordem, pelo *Collection Runner* (Postman) ou pelo *Runner* (Bruno). As alterações e exclusões ficam na última pasta porque mudam o rateio, o markup e o estoque. A exclusão do produto termina em 409 de propósito: o produto tem movimentações de estoque.

## Variáveis da collection

| Variável | Padrão | Uso |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | Endereço da API |
| `email` / `senha` | `carlos@lojaferramentas.com.br` / `Senha@2026` | Conta usada no cadastro e no login |
| `token` | preenchida pelo Login | Token JWT (válido por 60 minutos) |
| `produtoId`, `despesaId`, `movimentacaoId`, `dataComemorativaId` | preenchidas pelos cadastros | Ids usados nas consultas, alterações e exclusões |

## Observações

- Com o `spring-boot:test-run`, o banco é recriado a cada subida. Depois de reiniciar a API, rode o cadastro e o login de novo. Rodar o cadastro duas vezes na mesma execução da API dá 409 (e-mail já cadastrado), mas o login continua funcionando.
- Se o token expirar (401 "Não autenticado"), rode o **Login** de novo.
- Os exemplos seguem o estudo de caso da loja de ferramentas (seção 4.1 do artigo): markup 1,6667 e preço sugerido da furadeira 300,00.
