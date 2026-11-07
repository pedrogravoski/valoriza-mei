package br.com.valorizamei.shared.seguranca;

import java.util.UUID;

/**
 * Confirma se o MEI informado no token existe. Declarada aqui e implementada pela feature
 * que guarda os MEIs, para que {@code shared} não dependa de nenhuma feature.
 */
public interface VerificadorDeTenant {

	boolean existe(UUID tenantId);

}
