package br.com.valorizamei.alerta.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Alertas do MEI na data de referência: produtos em ruptura (RF08), datas comemorativas dos
 * próximos 30 dias e, se houver alguma, as sugestões pelo giro de cada produto (RF09). Listas
 * vazias indicam que não há alerta.
 */
public record AlertasResponse(LocalDate dataReferencia, List<RupturaResponse> ruptura,
		List<DataProximaResponse> datasComemorativas, List<SugestaoResponse> sugestoes) {

}
