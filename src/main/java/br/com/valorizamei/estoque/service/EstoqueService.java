package br.com.valorizamei.estoque.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.caixa.dto.TipoLancamento;
import br.com.valorizamei.caixa.service.CaixaService;
import br.com.valorizamei.estoque.dto.EstoqueProdutoResponse;
import br.com.valorizamei.estoque.dto.MovimentacaoRegistradaResponse;
import br.com.valorizamei.estoque.dto.MovimentacaoRequest;
import br.com.valorizamei.estoque.dto.MovimentacaoResponse;
import br.com.valorizamei.estoque.dto.TipoMovimentacao;
import br.com.valorizamei.estoque.dto.VendasDoProdutoResponse;
import br.com.valorizamei.estoque.factory.MovimentacaoEstoqueFactory;
import br.com.valorizamei.estoque.model.MovimentacaoEstoque;
import br.com.valorizamei.estoque.model.SaldoEstoque;
import br.com.valorizamei.estoque.model.SentidoMovimentacao;
import br.com.valorizamei.estoque.repository.MovimentacaoEstoqueRepository;
import br.com.valorizamei.estoque.repository.SaldoEstoqueRepository;
import br.com.valorizamei.produto.dto.ProdutoResponse;
import br.com.valorizamei.produto.service.ProdutoService;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;
import br.com.valorizamei.shared.seguranca.TenantAtual;

/**
 * Entradas (compras) e saídas (vendas) de estoque dos produtos do MEI do token (RF06, RNF07).
 * Cada movimentação atualiza o estoque atual, que nunca fica negativo, e gera, na mesma
 * transação, o lançamento correspondente no fluxo de caixa (RF07).
 */
@Service
public class EstoqueService {

	private static final Sort MAIS_RECENTES_PRIMEIRO = Sort.by(Sort.Order.desc("data"), Sort.Order.desc("criadoEm"));

	private final MovimentacaoEstoqueRepository movimentacaoRepository;

	private final SaldoEstoqueRepository saldoRepository;

	private final MovimentacaoEstoqueFactory movimentacaoFactory;

	private final ProdutoService produtoService;

	private final CaixaService caixaService;

	private final TenantAtual tenantAtual;

	private final Clock relogio;

	public EstoqueService(MovimentacaoEstoqueRepository movimentacaoRepository, SaldoEstoqueRepository saldoRepository,
			MovimentacaoEstoqueFactory movimentacaoFactory, ProdutoService produtoService, CaixaService caixaService,
			TenantAtual tenantAtual, Clock relogio) {
		this.movimentacaoRepository = movimentacaoRepository;
		this.saldoRepository = saldoRepository;
		this.movimentacaoFactory = movimentacaoFactory;
		this.produtoService = produtoService;
		this.caixaService = caixaService;
		this.tenantAtual = tenantAtual;
		this.relogio = relogio;
	}

	/**
	 * Na entrada, o valor unitário é o custo direto do produto e o caixa recebe uma saída; na
	 * saída, o valor é o da venda e o caixa recebe uma entrada. Uma saída maior que o estoque é
	 * recusada sem gravar nada.
	 */
	@Transactional
	public MovimentacaoRegistradaResponse registrar(MovimentacaoRequest dados) {
		UUID tenantId = tenantAtual.id();
		ProdutoResponse produto = produtoService.consultar(dados.produtoId());
		boolean entrada = dados.tipo() == TipoMovimentacao.ENTRADA;
		if (entrada) {
			saldoRepository.somar(produto.id(), tenantId, dados.quantidade());
		}
		else if (saldoRepository.subtrair(produto.id(), tenantId, dados.quantidade()) == 0) {
			throw estoqueInsuficiente(produto, quantidadeEmEstoque(tenantId, produto.id()), dados.quantidade());
		}
		BigDecimal valorUnitario = entrada ? produto.custoDireto() : dados.valorUnitario();
		LocalDate data = dados.data() != null ? dados.data() : LocalDate.now(relogio);
		MovimentacaoEstoque movimentacao = movimentacaoRepository.save(
				movimentacaoFactory.paraMovimentacao(dados, tenantId, valorUnitario, data, relogio.instant()));
		caixaService.lancar(entrada ? TipoLancamento.SAIDA : TipoLancamento.ENTRADA,
				descricaoDoLancamento(entrada, dados.quantidade(), produto.nome()), movimentacao.getValorTotal(), data,
				movimentacao.getId());
		return movimentacaoFactory.paraRegistrada(movimentacao, produto.nome(),
				quantidadeEmEstoque(tenantId, produto.id()));
	}

	/** Movimentações do MEI, da mais recente para a mais antiga; com {@code produtoId}, só as desse produto. */
	@Transactional(readOnly = true)
	public List<MovimentacaoResponse> listarMovimentacoes(UUID produtoId) {
		UUID tenantId = tenantAtual.id();
		List<MovimentacaoEstoque> movimentacoes = produtoId == null
				? movimentacaoRepository.findAllByTenantId(tenantId, MAIS_RECENTES_PRIMEIRO)
				: movimentacaoRepository.findAllByTenantIdAndProdutoId(tenantId, produtoId, MAIS_RECENTES_PRIMEIRO);
		Map<UUID, String> nomes = produtoService.listar().stream()
			.collect(Collectors.toMap(ProdutoResponse::id, ProdutoResponse::nome));
		return movimentacoes.stream()
			.map(movimentacao -> movimentacaoFactory.paraResponse(movimentacao, nomes.get(movimentacao.getProdutoId())))
			.toList();
	}

	@Transactional(readOnly = true)
	public MovimentacaoResponse consultarMovimentacao(UUID id) {
		MovimentacaoEstoque movimentacao = movimentacaoRepository.findByIdAndTenantId(id, tenantAtual.id())
			.orElseThrow(EstoqueService::movimentacaoNaoEncontrada);
		return movimentacaoFactory.paraResponse(movimentacao,
				produtoService.consultar(movimentacao.getProdutoId()).nome());
	}

	/** Todos os produtos do MEI em ordem alfabética, com estoque atual (0 sem movimentações) e mínimo. */
	@Transactional(readOnly = true)
	public List<EstoqueProdutoResponse> estoqueAtual() {
		Map<UUID, Integer> saldos = saldoRepository.findAllByTenantId(tenantAtual.id()).stream()
			.collect(Collectors.toMap(SaldoEstoque::getProdutoId, SaldoEstoque::getQuantidade));
		return produtoService.listar().stream()
			.map(produto -> movimentacaoFactory.paraEstoque(produto, saldos.getOrDefault(produto.id(), 0)))
			.toList();
	}

	/** Unidades vendidas do produto entre as datas (inclusivas), para os alertas (RF08, RF09). */
	@Transactional(readOnly = true)
	public long quantidadeVendida(UUID produtoId, LocalDate de, LocalDate ate) {
		return movimentacaoRepository.somarQuantidade(tenantAtual.id(), produtoId, SentidoMovimentacao.SAIDA, de, ate);
	}

	/**
	 * Unidades e valor vendidos por produto entre as datas (inclusivas), para o giro dos alertas
	 * (RF09). Produtos sem vendas no período não aparecem.
	 */
	@Transactional(readOnly = true)
	public List<VendasDoProdutoResponse> vendasPorProduto(LocalDate de, LocalDate ate) {
		return movimentacaoRepository.somarPorProduto(tenantAtual.id(), SentidoMovimentacao.SAIDA, de, ate).stream()
			.map(movimentacaoFactory::paraVendas)
			.toList();
	}

	private int quantidadeEmEstoque(UUID tenantId, UUID produtoId) {
		return saldoRepository.findByProdutoIdAndTenantId(produtoId, tenantId)
			.map(SaldoEstoque::getQuantidade)
			.orElse(0);
	}

	private static String descricaoDoLancamento(boolean entrada, int quantidade, String nomeProduto) {
		return (entrada ? "Compra: " : "Venda: ") + quantidade + " × " + nomeProduto;
	}

	private static ErroDeNegocioException estoqueInsuficiente(ProdutoResponse produto, int emEstoque, int pedida) {
		return new ErroDeNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "Estoque insuficiente",
				"O produto " + produto.nome() + " tem " + unidades(emEstoque)
						+ " em estoque; não é possível registrar a saída de " + pedida + ".");
	}

	private static String unidades(int quantidade) {
		return quantidade + (quantidade == 1 ? " unidade" : " unidades");
	}

	private static ErroDeNegocioException movimentacaoNaoEncontrada() {
		return new ErroDeNegocioException(HttpStatus.NOT_FOUND, "Movimentação não encontrada",
				"Não existe movimentação de estoque com este identificador.");
	}

}
