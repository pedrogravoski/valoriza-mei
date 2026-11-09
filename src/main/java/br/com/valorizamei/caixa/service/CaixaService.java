package br.com.valorizamei.caixa.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.caixa.dto.FluxoDeCaixaResponse;
import br.com.valorizamei.caixa.dto.LancamentoResponse;
import br.com.valorizamei.caixa.dto.TipoLancamento;
import br.com.valorizamei.caixa.factory.LancamentoCaixaFactory;
import br.com.valorizamei.caixa.model.LancamentoCaixa;
import br.com.valorizamei.caixa.model.SentidoLancamento;
import br.com.valorizamei.caixa.repository.LancamentoCaixaRepository;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;
import br.com.valorizamei.shared.seguranca.TenantAtual;

/**
 * Fluxo de caixa do MEI do token (RF07, RNF07): grava os lançamentos gerados pelas
 * movimentações de estoque e os consulta por período, com os totais e o saldo.
 */
@Service
public class CaixaService {

	private static final Sort CRONOLOGICA = Sort.by("data", "criadoEm");

	private final LancamentoCaixaRepository lancamentoRepository;

	private final LancamentoCaixaFactory lancamentoFactory;

	private final TenantAtual tenantAtual;

	private final Clock relogio;

	public CaixaService(LancamentoCaixaRepository lancamentoRepository, LancamentoCaixaFactory lancamentoFactory,
			TenantAtual tenantAtual, Clock relogio) {
		this.lancamentoRepository = lancamentoRepository;
		this.lancamentoFactory = lancamentoFactory;
		this.tenantAtual = tenantAtual;
		this.relogio = relogio;
	}

	/** Grava o lançamento para o MEI do token, na transação de quem chama (a movimentação de estoque). */
	@Transactional
	public void lancar(TipoLancamento tipo, String descricao, BigDecimal valor, LocalDate data,
			UUID movimentacaoEstoqueId) {
		lancamentoRepository.save(lancamentoFactory.paraLancamento(tenantAtual.id(), tipo, descricao, valor, data,
				movimentacaoEstoqueId, relogio.instant()));
	}

	/** Sem datas informadas, usa o mês da data atual; as datas são inclusivas. */
	@Transactional(readOnly = true)
	public FluxoDeCaixaResponse consultar(LocalDate inicioInformado, LocalDate fimInformado) {
		YearMonth mesAtual = YearMonth.now(relogio);
		LocalDate inicio = inicioInformado != null ? inicioInformado : mesAtual.atDay(1);
		LocalDate fim = fimInformado != null ? fimInformado : mesAtual.atEndOfMonth();
		if (inicio.isAfter(fim)) {
			throw new ErroDeNegocioException(HttpStatus.BAD_REQUEST, "Período inválido",
					"A data de início precisa ser igual ou anterior à data de fim.");
		}
		List<LancamentoCaixa> lancamentos = lancamentoRepository.findAllByTenantIdAndDataBetween(tenantAtual.id(),
				inicio, fim, CRONOLOGICA);
		List<LancamentoResponse> respostas = lancamentos.stream().map(lancamentoFactory::paraResponse).toList();
		return lancamentoFactory.paraFluxo(inicio, fim, somar(lancamentos, SentidoLancamento.ENTRADA),
				somar(lancamentos, SentidoLancamento.SAIDA), respostas);
	}

	private static BigDecimal somar(List<LancamentoCaixa> lancamentos, SentidoLancamento sentido) {
		return lancamentos.stream()
			.filter(lancamento -> lancamento.getSentido() == sentido)
			.map(LancamentoCaixa::getValor)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}
