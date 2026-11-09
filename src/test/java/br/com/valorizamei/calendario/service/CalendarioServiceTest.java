package br.com.valorizamei.calendario.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Datas fixas, próxima ocorrência e janela de 30 dias do calendário (spec datas-comemorativas e
 * alertas; RF09).
 */
class CalendarioServiceTest {

	@Test
	void datasFixasDe2026() {
		Map<String, LocalDate> datas = CalendarioService.datasFixas(2026);

		assertThat(datas).containsExactly(
				Map.entry("Dia das Mães", LocalDate.of(2026, 5, 10)),
				Map.entry("Dia dos Pais", LocalDate.of(2026, 8, 9)),
				Map.entry("Dia das Crianças", LocalDate.of(2026, 10, 12)),
				Map.entry("Black Friday", LocalDate.of(2026, 11, 27)),
				Map.entry("Natal", LocalDate.of(2026, 12, 25)));
	}

	@Test
	void datasFixasDe2027() {
		Map<String, LocalDate> datas = CalendarioService.datasFixas(2027);

		assertThat(datas)
			.containsEntry("Dia das Mães", LocalDate.of(2027, 5, 9))
			.containsEntry("Dia dos Pais", LocalDate.of(2027, 8, 8))
			.containsEntry("Black Friday", LocalDate.of(2027, 11, 26));
	}

	@Test
	void proximaOcorrenciaNoMesmoAno() {
		assertThat(CalendarioService.proximaOcorrencia(MonthDay.of(8, 1), LocalDate.of(2026, 7, 15)))
			.isEqualTo(LocalDate.of(2026, 8, 1));
	}

	@Test
	void proximaOcorrenciaNoAnoSeguinte() {
		assertThat(CalendarioService.proximaOcorrencia(MonthDay.of(1, 5), LocalDate.of(2026, 7, 15)))
			.isEqualTo(LocalDate.of(2027, 1, 5));
	}

	@Test
	void proximaOcorrenciaNoProprioDia() {
		assertThat(CalendarioService.proximaOcorrencia(MonthDay.of(7, 15), LocalDate.of(2026, 7, 15)))
			.isEqualTo(LocalDate.of(2026, 7, 15));
	}

	@Test
	void dataFixaJaPassadaVaiParaOAnoSeguinte() {
		LocalDate diaDasMaes = CalendarioService.proximaOcorrencia(ano -> CalendarioService.datasFixas(ano)
			.get("Dia das Mães"), LocalDate.of(2026, 7, 15));

		assertThat(diaDasMaes).isEqualTo(LocalDate.of(2027, 5, 9));
	}

	@Test
	void limiteDeTrintaDias() {
		LocalDate diaDosPais = LocalDate.of(2026, 8, 9);

		assertThat(CalendarioService.estaProxima(diaDosPais, LocalDate.of(2026, 7, 10))).isTrue();
		assertThat(CalendarioService.estaProxima(diaDosPais, LocalDate.of(2026, 7, 9))).isFalse();
		assertThat(CalendarioService.estaProxima(diaDosPais, LocalDate.of(2026, 7, 15))).isTrue();
		assertThat(CalendarioService.estaProxima(diaDosPais, diaDosPais)).isTrue();
	}

	@Test
	void nenhumaDataFixaProximaEm15DeJunho() {
		LocalDate hoje = LocalDate.of(2026, 6, 15);

		assertThat(CalendarioService.datasFixas(2026).values())
			.noneMatch(data -> CalendarioService.estaProxima(data, hoje));
	}

}
