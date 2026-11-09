package br.com.valorizamei.despesa.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.despesa.dto.ParametrosFinanceirosRequest;
import br.com.valorizamei.despesa.dto.ParametrosFinanceirosResponse;
import br.com.valorizamei.despesa.factory.ParametrosFinanceirosFactory;
import br.com.valorizamei.despesa.model.ParametrosFinanceiros;
import br.com.valorizamei.despesa.repository.ParametrosFinanceirosRepository;
import br.com.valorizamei.shared.seguranca.TenantAtual;

/**
 * Receita mensal estimada e margem de lucro do MEI do token (RF03, RF05, RNF07).
 */
@Service
public class ParametrosFinanceirosService {

	private final ParametrosFinanceirosRepository parametrosRepository;

	private final ParametrosFinanceirosFactory parametrosFactory;

	private final TenantAtual tenantAtual;

	private final Clock relogio;

	public ParametrosFinanceirosService(ParametrosFinanceirosRepository parametrosRepository,
			ParametrosFinanceirosFactory parametrosFactory, TenantAtual tenantAtual, Clock relogio) {
		this.parametrosRepository = parametrosRepository;
		this.parametrosFactory = parametrosFactory;
		this.tenantAtual = tenantAtual;
		this.relogio = relogio;
	}

	@Transactional(readOnly = true)
	public ParametrosFinanceirosResponse consultar() {
		return parametrosRepository.findById(tenantAtual.id())
			.map(parametrosFactory::paraResponse)
			.orElseGet(parametrosFactory::naoInformados);
	}

	/** Cria os parâmetros do MEI na primeira gravação e os substitui nas seguintes. */
	@Transactional
	public ParametrosFinanceirosResponse gravar(ParametrosFinanceirosRequest dados) {
		UUID tenantId = tenantAtual.id();
		Instant agora = relogio.instant();
		Optional<ParametrosFinanceiros> existentes = parametrosRepository.findById(tenantId);
		ParametrosFinanceiros parametros;
		if (existentes.isPresent()) {
			parametros = existentes.get();
			parametrosFactory.atualizar(parametros, dados, agora);
		}
		else {
			parametros = parametrosRepository.save(parametrosFactory.paraParametros(dados, tenantId, agora));
		}
		return parametrosFactory.paraResponse(parametros);
	}

}
