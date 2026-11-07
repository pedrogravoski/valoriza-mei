package br.com.valorizamei.despesa.factory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.valorizamei.despesa.dto.ParametrosFinanceirosRequest;
import br.com.valorizamei.despesa.dto.ParametrosFinanceirosResponse;
import br.com.valorizamei.despesa.model.ParametrosFinanceiros;

@Component
public class ParametrosFinanceirosFactory {

	public ParametrosFinanceiros paraParametros(ParametrosFinanceirosRequest dados, UUID tenantId,
			Instant atualizadoEm) {
		return new ParametrosFinanceiros(tenantId, comDuasCasas(dados.receitaMensalEstimada()),
				comDuasCasas(dados.margemDeLucro()), atualizadoEm);
	}

	public void atualizar(ParametrosFinanceiros parametros, ParametrosFinanceirosRequest dados,
			Instant atualizadoEm) {
		parametros.alterar(comDuasCasas(dados.receitaMensalEstimada()), comDuasCasas(dados.margemDeLucro()),
				atualizadoEm);
	}

	public ParametrosFinanceirosResponse paraResponse(ParametrosFinanceiros parametros) {
		return new ParametrosFinanceirosResponse(parametros.getReceitaMensalEstimada(),
				parametros.getMargemDeLucro());
	}

	public ParametrosFinanceirosResponse naoInformados() {
		return new ParametrosFinanceirosResponse(null, null);
	}

	/** A validação garante no máximo 2 casas; aqui só se completa a escala (25 vira 25.00). */
	static BigDecimal comDuasCasas(BigDecimal valor) {
		return valor == null ? null : valor.setScale(2, RoundingMode.UNNECESSARY);
	}

}
