package br.com.valorizamei.despesa.factory;

import static br.com.valorizamei.despesa.factory.ParametrosFinanceirosFactory.comDuasCasas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.valorizamei.despesa.dto.DespesaRequest;
import br.com.valorizamei.despesa.dto.DespesaResponse;
import br.com.valorizamei.despesa.dto.RateioResponse;
import br.com.valorizamei.despesa.dto.TipoDespesa;
import br.com.valorizamei.despesa.model.ClassificacaoDespesa;
import br.com.valorizamei.despesa.model.Despesa;

@Component
public class DespesaFactory {

	public Despesa paraDespesa(DespesaRequest dados, UUID tenantId, Instant criadoEm) {
		return new Despesa(tenantId, dados.descricao(), paraClassificacao(dados.tipo()),
				comDuasCasas(dados.valorMensal()), comDuasCasas(dados.percentual()), criadoEm);
	}

	public void atualizar(Despesa despesa, DespesaRequest dados, Instant atualizadoEm) {
		despesa.alterar(dados.descricao(), paraClassificacao(dados.tipo()), comDuasCasas(dados.valorMensal()),
				comDuasCasas(dados.percentual()), atualizadoEm);
	}

	public DespesaResponse paraResponse(Despesa despesa) {
		return new DespesaResponse(despesa.getId(), despesa.getDescricao(),
				TipoDespesa.valueOf(despesa.getClassificacao().name()), despesa.getValorMensal(),
				despesa.getPercentual());
	}

	public ClassificacaoDespesa paraClassificacao(TipoDespesa tipo) {
		return ClassificacaoDespesa.valueOf(tipo.name());
	}

	/** Arredonda para exibição (2 casas, meio para cima) os valores calculados pelo rateio. */
	public RateioResponse paraRateio(BigDecimal totalDespesasFixas, BigDecimal receitaMensalEstimada,
			BigDecimal percentualDespesasFixas, BigDecimal percentualDespesasVariaveis) {
		return new RateioResponse(paraExibicao(totalDespesasFixas), receitaMensalEstimada,
				paraExibicao(percentualDespesasFixas), paraExibicao(percentualDespesasVariaveis));
	}

	private static BigDecimal paraExibicao(BigDecimal valor) {
		return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
	}

}
