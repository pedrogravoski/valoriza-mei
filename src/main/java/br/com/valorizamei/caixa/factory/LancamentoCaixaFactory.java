package br.com.valorizamei.caixa.factory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.valorizamei.caixa.dto.FluxoDeCaixaResponse;
import br.com.valorizamei.caixa.dto.LancamentoResponse;
import br.com.valorizamei.caixa.dto.TipoLancamento;
import br.com.valorizamei.caixa.model.LancamentoCaixa;
import br.com.valorizamei.caixa.model.SentidoLancamento;

@Component
public class LancamentoCaixaFactory {

	public LancamentoCaixa paraLancamento(UUID tenantId, TipoLancamento tipo, String descricao, BigDecimal valor,
			LocalDate data, UUID movimentacaoEstoqueId, Instant criadoEm) {
		return new LancamentoCaixa(tenantId, SentidoLancamento.valueOf(tipo.name()), descricao,
				valor.setScale(2, RoundingMode.UNNECESSARY), data, movimentacaoEstoqueId, criadoEm);
	}

	public LancamentoResponse paraResponse(LancamentoCaixa lancamento) {
		return new LancamentoResponse(lancamento.getId(), lancamento.getData(),
				TipoLancamento.valueOf(lancamento.getSentido().name()), lancamento.getDescricao(),
				lancamento.getValor());
	}

	public FluxoDeCaixaResponse paraFluxo(LocalDate inicio, LocalDate fim, BigDecimal totalEntradas,
			BigDecimal totalSaidas, List<LancamentoResponse> lancamentos) {
		BigDecimal entradas = totalEntradas.setScale(2, RoundingMode.UNNECESSARY);
		BigDecimal saidas = totalSaidas.setScale(2, RoundingMode.UNNECESSARY);
		return new FluxoDeCaixaResponse(inicio, fim, entradas, saidas, entradas.subtract(saidas), lancamentos);
	}

}
