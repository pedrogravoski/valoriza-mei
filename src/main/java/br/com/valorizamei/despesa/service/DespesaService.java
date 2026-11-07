package br.com.valorizamei.despesa.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.despesa.dto.DespesaRequest;
import br.com.valorizamei.despesa.dto.DespesaResponse;
import br.com.valorizamei.despesa.dto.TipoDespesa;
import br.com.valorizamei.despesa.factory.DespesaFactory;
import br.com.valorizamei.despesa.model.Despesa;
import br.com.valorizamei.despesa.repository.DespesaRepository;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;
import br.com.valorizamei.shared.seguranca.TenantAtual;

/**
 * Registro das despesas fixas e variáveis do MEI (RF02). Toda operação considera somente as
 * despesas do MEI do token: uma despesa de outro MEI é tratada como inexistente (RNF07).
 */
@Service
public class DespesaService {

	private static final Sort POR_DESCRICAO = Sort.by(Sort.Order.asc("descricao").ignoreCase());

	private final DespesaRepository despesaRepository;

	private final DespesaFactory despesaFactory;

	private final TenantAtual tenantAtual;

	private final Clock relogio;

	public DespesaService(DespesaRepository despesaRepository, DespesaFactory despesaFactory,
			TenantAtual tenantAtual, Clock relogio) {
		this.despesaRepository = despesaRepository;
		this.despesaFactory = despesaFactory;
		this.tenantAtual = tenantAtual;
		this.relogio = relogio;
	}

	@Transactional
	public DespesaResponse registrar(DespesaRequest dados) {
		UUID tenantId = tenantAtual.id();
		if (despesaRepository.existsByTenantIdAndDescricaoIgnoreCase(tenantId, dados.descricao())) {
			throw despesaJaCadastrada();
		}
		Despesa despesa = despesaFactory.paraDespesa(dados, tenantId, relogio.instant());
		return despesaFactory.paraResponse(gravar(despesa));
	}

	/** Lista as despesas do MEI pela descrição; com {@code tipo} nulo, lista todas. */
	@Transactional(readOnly = true)
	public List<DespesaResponse> listar(TipoDespesa tipo) {
		UUID tenantId = tenantAtual.id();
		List<Despesa> despesas = tipo == null
				? despesaRepository.findAllByTenantId(tenantId, POR_DESCRICAO)
				: despesaRepository.findAllByTenantIdAndClassificacao(tenantId,
						despesaFactory.paraClassificacao(tipo), POR_DESCRICAO);
		return despesas.stream().map(despesaFactory::paraResponse).toList();
	}

	@Transactional(readOnly = true)
	public DespesaResponse consultar(UUID id) {
		return despesaFactory.paraResponse(buscarDoMei(id));
	}

	@Transactional
	public DespesaResponse alterar(UUID id, DespesaRequest dados) {
		Despesa despesa = buscarDoMei(id);
		if (despesaRepository.existsByTenantIdAndDescricaoIgnoreCaseAndIdNot(despesa.getTenantId(),
				dados.descricao(), id)) {
			throw despesaJaCadastrada();
		}
		despesaFactory.atualizar(despesa, dados, relogio.instant());
		return despesaFactory.paraResponse(gravar(despesa));
	}

	@Transactional
	public void excluir(UUID id) {
		despesaRepository.delete(buscarDoMei(id));
	}

	private Despesa buscarDoMei(UUID id) {
		return despesaRepository.findByIdAndTenantId(id, tenantAtual.id())
			.orElseThrow(DespesaService::despesaNaoEncontrada);
	}

	private Despesa gravar(Despesa despesa) {
		try {
			return despesaRepository.saveAndFlush(despesa);
		}
		catch (DataIntegrityViolationException ex) {
			// Outra despesa com a mesma descrição foi gravada entre a verificação e a gravação.
			throw despesaJaCadastrada();
		}
	}

	private static ErroDeNegocioException despesaNaoEncontrada() {
		return new ErroDeNegocioException(HttpStatus.NOT_FOUND, "Despesa não encontrada",
				"Não existe despesa com este identificador.");
	}

	private static ErroDeNegocioException despesaJaCadastrada() {
		return new ErroDeNegocioException(HttpStatus.CONFLICT, "Despesa já cadastrada",
				"Já existe uma despesa com esta descrição.");
	}

}
