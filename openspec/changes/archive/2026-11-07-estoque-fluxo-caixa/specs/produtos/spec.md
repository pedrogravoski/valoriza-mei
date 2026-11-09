# Spec Delta

## ADDED Requirements

### Requirement: Produto com movimentações não pode ser excluído

A exclusão de um produto que tenha movimentações de estoque MUST ser recusada com status 409 no formato padrão de erro, com `title` e `detail` em português, para preservar o histórico de estoque e de caixa. O produto continua cadastrado e inalterado. Produtos sem movimentações continuam podendo ser excluídos.

Atende: RF01, RF06.

#### Scenario: Exclusão da furadeira com compras registradas

- **WHEN** a "Loja de Ferramentas" registrou a entrada de 10 unidades da "Furadeira de impacto 650 W"
- **AND** envia `DELETE /api/v1/produtos/{id}` para a furadeira
- **THEN** o sistema responde com status 409 com `title` "Produto com movimentações"
- **AND** `GET /api/v1/produtos/{id}` continua respondendo 200 com a furadeira
