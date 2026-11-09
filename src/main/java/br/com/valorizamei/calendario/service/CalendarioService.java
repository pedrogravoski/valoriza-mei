package br.com.valorizamei.calendario.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.calendario.dto.DataComemorativaRequest;
import br.com.valorizamei.calendario.dto.DataComemorativaResponse;
import br.com.valorizamei.calendario.factory.DataComemorativaFactory;
import br.com.valorizamei.calendario.model.DataComemorativa;
import br.com.valorizamei.calendario.repository.DataComemorativaRepository;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;
import br.com.valorizamei.shared.seguranca.TenantAtual;

/**
 * Calendário de datas comemorativas (RF09): as datas fixas do varejo, calculadas para qualquer
 * ano, e as datas personalizadas do MEI do token (RNF07), repetidas todo ano. Uma data é
 * próxima quando ocorre entre hoje e {@value #DIAS_DE_ANTECEDENCIA} dias depois.
 */
@Service
public class CalendarioService {

	static final int DIAS_DE_ANTECEDENCIA = 30;

	/** Datas fixas, calculadas para um ano; a ordem é a do ano. */
	private static final Map<String, IntFunction<LocalDate>> DATAS_FIXAS = datasFixasPorNome();

	private static final Comparator<DataComemorativaResponse> POR_PROXIMA_OCORRENCIA = Comparator
		.comparing(DataComemorativaResponse::proximaOcorrencia)
		.thenComparing(DataComemorativaResponse::nome, String.CASE_INSENSITIVE_ORDER);

	private final DataComemorativaRepository dataRepository;

	private final DataComemorativaFactory dataFactory;

	private final TenantAtual tenantAtual;

	private final Clock relogio;

	public CalendarioService(DataComemorativaRepository dataRepository, DataComemorativaFactory dataFactory,
			TenantAtual tenantAtual, Clock relogio) {
		this.dataRepository = dataRepository;
		this.dataFactory = dataFactory;
		this.tenantAtual = tenantAtual;
		this.relogio = relogio;
	}

	/** Datas fixas e personalizadas do MEI, ordenadas pela próxima ocorrência a partir de hoje. */
	@Transactional(readOnly = true)
	public List<DataComemorativaResponse> calendario() {
		LocalDate hoje = LocalDate.now(relogio);
		List<DataComemorativaResponse> datas = new ArrayList<>();
		DATAS_FIXAS.forEach((nome, noAno) -> datas.add(dataFactory.paraFixa(nome, proximaOcorrencia(noAno, hoje), hoje)));
		dataRepository.findAllByTenantId(tenantAtual.id()).forEach(data -> datas.add(paraResponse(data, hoje)));
		datas.sort(POR_PROXIMA_OCORRENCIA);
		return datas;
	}

	/** Datas do calendário que ocorrem entre hoje e {@value #DIAS_DE_ANTECEDENCIA} dias depois. */
	@Transactional(readOnly = true)
	public List<DataComemorativaResponse> proximasDatas() {
		LocalDate hoje = LocalDate.now(relogio);
		return calendario().stream()
			.filter(data -> estaProxima(data.proximaOcorrencia(), hoje))
			.toList();
	}

	@Transactional
	public DataComemorativaResponse cadastrar(DataComemorativaRequest dados) {
		UUID tenantId = tenantAtual.id();
		if (dataRepository.existsByTenantIdAndNomeIgnoreCase(tenantId, dados.nome())) {
			throw dataJaCadastrada();
		}
		DataComemorativa data = gravar(dataFactory.paraData(dados, tenantId, relogio.instant()));
		return paraResponse(data, LocalDate.now(relogio));
	}

	@Transactional(readOnly = true)
	public DataComemorativaResponse consultar(UUID id) {
		return paraResponse(buscarDoMei(id), LocalDate.now(relogio));
	}

	@Transactional
	public DataComemorativaResponse alterar(UUID id, DataComemorativaRequest dados) {
		DataComemorativa data = buscarDoMei(id);
		if (dataRepository.existsByTenantIdAndNomeIgnoreCaseAndIdNot(data.getTenantId(), dados.nome(), id)) {
			throw dataJaCadastrada();
		}
		dataFactory.atualizar(data, dados, relogio.instant());
		return paraResponse(gravar(data), LocalDate.now(relogio));
	}

	@Transactional
	public void excluir(UUID id) {
		dataRepository.delete(buscarDoMei(id));
	}

	/** Datas fixas do ano informado, pelo nome, na ordem do ano. */
	static Map<String, LocalDate> datasFixas(int ano) {
		Map<String, LocalDate> datas = new LinkedHashMap<>();
		DATAS_FIXAS.forEach((nome, noAno) -> datas.put(nome, noAno.apply(ano)));
		return datas;
	}

	/** A data no ano de hoje, se ainda não passou (hoje inclusive); senão, no ano seguinte. */
	static LocalDate proximaOcorrencia(IntFunction<LocalDate> noAno, LocalDate hoje) {
		LocalDate nesteAno = noAno.apply(hoje.getYear());
		return nesteAno.isBefore(hoje) ? noAno.apply(hoje.getYear() + 1) : nesteAno;
	}

	static LocalDate proximaOcorrencia(MonthDay diaDoAno, LocalDate hoje) {
		return proximaOcorrencia(diaDoAno::atYear, hoje);
	}

	static boolean estaProxima(LocalDate data, LocalDate hoje) {
		return !data.isBefore(hoje) && !data.isAfter(hoje.plusDays(DIAS_DE_ANTECEDENCIA));
	}

	private static Map<String, IntFunction<LocalDate>> datasFixasPorNome() {
		Map<String, IntFunction<LocalDate>> datas = new LinkedHashMap<>();
		datas.put("Dia das Mães", ano -> segundoDomingo(ano, Month.MAY));
		datas.put("Dia dos Pais", ano -> segundoDomingo(ano, Month.AUGUST));
		datas.put("Dia das Crianças", ano -> LocalDate.of(ano, Month.OCTOBER, 12));
		datas.put("Black Friday", CalendarioService::blackFriday);
		datas.put("Natal", ano -> LocalDate.of(ano, Month.DECEMBER, 25));
		return datas;
	}

	private static LocalDate segundoDomingo(int ano, Month mes) {
		return LocalDate.of(ano, mes, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY));
	}

	/** Sexta-feira seguinte à 4ª quinta-feira de novembro. */
	private static LocalDate blackFriday(int ano) {
		return LocalDate.of(ano, Month.NOVEMBER, 1)
			.with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))
			.plusDays(1);
	}

	private DataComemorativaResponse paraResponse(DataComemorativa data, LocalDate hoje) {
		return dataFactory.paraPersonalizada(data, proximaOcorrencia(data.getDiaDoAno(), hoje), hoje);
	}

	private DataComemorativa buscarDoMei(UUID id) {
		return dataRepository.findByIdAndTenantId(id, tenantAtual.id())
			.orElseThrow(CalendarioService::dataNaoEncontrada);
	}

	private DataComemorativa gravar(DataComemorativa data) {
		try {
			return dataRepository.saveAndFlush(data);
		}
		catch (DataIntegrityViolationException ex) {
			// Outra data com o mesmo nome foi gravada entre a verificação e a gravação.
			throw dataJaCadastrada();
		}
	}

	private static ErroDeNegocioException dataNaoEncontrada() {
		return new ErroDeNegocioException(HttpStatus.NOT_FOUND, "Data comemorativa não encontrada",
				"Não existe data comemorativa personalizada com este identificador.");
	}

	private static ErroDeNegocioException dataJaCadastrada() {
		return new ErroDeNegocioException(HttpStatus.CONFLICT, "Data comemorativa já cadastrada",
				"Já existe uma data comemorativa com este nome.");
	}

}
