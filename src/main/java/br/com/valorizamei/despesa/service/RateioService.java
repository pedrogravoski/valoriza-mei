package br.com.valorizamei.despesa.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.despesa.dto.RateioResponse;
import br.com.valorizamei.despesa.factory.DespesaFactory;
import br.com.valorizamei.despesa.model.ClassificacaoDespesa;
import br.com.valorizamei.despesa.model.Despesa;
import br.com.valorizamei.despesa.model.ParametrosFinanceiros;
import br.com.valorizamei.despesa.repository.DespesaRepository;
import br.com.valorizamei.despesa.repository.ParametrosFinanceirosRepository;
import br.com.valorizamei.shared.seguranca.TenantAtual;

/**
 * Rateio das despesas do MEI do token (RF05), em pontos percentuais:
 * DF = soma das despesas fixas mensais × 100 / receita mensal estimada, e DV = soma dos
 * percentuais das despesas variáveis. A resposta da API arredonda para 2 casas; o cálculo do
 * markup usa {@link #percentualDespesasFixas()}, sem esse arredondamento.
 */
@Service
public class RateioService {

	/** Casas decimais do DF antes do arredondamento de exibição. */
	static final int ESCALA_DO_CALCULO = 10;

	private static final BigDecimal CEM = BigDecimal.valueOf(100);

	private final DespesaRepository despesaRepository;

	private final ParametrosFinanceirosRepository parametrosRepository;

	private final DespesaFactory despesaFactory;

	private final TenantAtual tenantAtual;

	public RateioService(DespesaRepository despesaRepository, ParametrosFinanceirosRepository parametrosRepository,
			DespesaFactory despesaFactory, TenantAtual tenantAtual) {
		this.despesaRepository = despesaRepository;
		this.parametrosRepository = parametrosRepository;
		this.despesaFactory = despesaFactory;
		this.tenantAtual = tenantAtual;
	}

	@Transactional(readOnly = true)
	public RateioResponse consultar() {
		UUID tenantId = tenantAtual.id();
		BigDecimal totalFixas = totalDespesasFixas(tenantId);
		Optional<BigDecimal> receita = receitaMensalEstimada(tenantId);
		BigDecimal df = receita.map(valor -> percentualSobreReceita(totalFixas, valor)).orElse(null);
		return despesaFactory.paraRateio(totalFixas, receita.orElse(null), df,
				percentualDespesasVariaveis(tenantId));
	}

	/** DF sem arredondamento; vazio enquanto o MEI não informar a receita mensal estimada. */
	@Transactional(readOnly = true)
	public Optional<BigDecimal> percentualDespesasFixas() {
		UUID tenantId = tenantAtual.id();
		BigDecimal totalFixas = totalDespesasFixas(tenantId);
		return receitaMensalEstimada(tenantId).map(receita -> percentualSobreReceita(totalFixas, receita));
	}

	@Transactional(readOnly = true)
	public BigDecimal percentualDespesasVariaveis() {
		return percentualDespesasVariaveis(tenantAtual.id());
	}

	static BigDecimal percentualSobreReceita(BigDecimal totalDespesasFixas, BigDecimal receitaMensalEstimada) {
		return totalDespesasFixas.multiply(CEM).divide(receitaMensalEstimada, ESCALA_DO_CALCULO, RoundingMode.HALF_UP);
	}

	private BigDecimal totalDespesasFixas(UUID tenantId) {
		return somar(tenantId, ClassificacaoDespesa.FIXA, Despesa::getValorMensal);
	}

	private BigDecimal percentualDespesasVariaveis(UUID tenantId) {
		return somar(tenantId, ClassificacaoDespesa.VARIAVEL, Despesa::getPercentual);
	}

	private BigDecimal somar(UUID tenantId, ClassificacaoDespesa classificacao, Function<Despesa, BigDecimal> valor) {
		List<Despesa> despesas = despesaRepository.findAllByTenantIdAndClassificacao(tenantId, classificacao,
				Sort.unsorted());
		return despesas.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private Optional<BigDecimal> receitaMensalEstimada(UUID tenantId) {
		return parametrosRepository.findById(tenantId).map(ParametrosFinanceiros::getReceitaMensalEstimada);
	}

}
