package br.com.valorizamei.calendario.factory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.valorizamei.calendario.dto.DataComemorativaRequest;
import br.com.valorizamei.calendario.dto.DataComemorativaResponse;
import br.com.valorizamei.calendario.model.DataComemorativa;

@Component
public class DataComemorativaFactory {

	public DataComemorativa paraData(DataComemorativaRequest dados, UUID tenantId, Instant criadoEm) {
		return new DataComemorativa(tenantId, dados.nome(), dados.dia(), dados.mes(), criadoEm);
	}

	public void atualizar(DataComemorativa data, DataComemorativaRequest dados, Instant atualizadoEm) {
		data.alterar(dados.nome(), dados.dia(), dados.mes(), atualizadoEm);
	}

	public DataComemorativaResponse paraPersonalizada(DataComemorativa data, LocalDate proximaOcorrencia,
			LocalDate hoje) {
		return new DataComemorativaResponse(data.getId(), data.getNome(), data.getDia(), data.getMes(),
				proximaOcorrencia, ChronoUnit.DAYS.between(hoje, proximaOcorrencia), true);
	}

	public DataComemorativaResponse paraFixa(String nome, LocalDate proximaOcorrencia, LocalDate hoje) {
		return new DataComemorativaResponse(null, nome, proximaOcorrencia.getDayOfMonth(),
				proximaOcorrencia.getMonthValue(), proximaOcorrencia, ChronoUnit.DAYS.between(hoje, proximaOcorrencia),
				false);
	}

}
