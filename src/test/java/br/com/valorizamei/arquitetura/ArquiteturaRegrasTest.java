package br.com.valorizamei.arquitetura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Prova cada regra de {@link ArquiteturaRegras} contra as fixtures de
 * {@code arquitetura.fixtures}: cada caso é um pacote raiz próprio.
 */
class ArquiteturaRegrasTest {

	private static final String FIXTURES = "br.com.valorizamei.arquitetura.fixtures";

	@Nested
	class PacoteBaseEOrganizacao {

		@Test
		void classeForaDoPacoteBase() {
			JavaClasses classes = importar("forapacote");
			violacao(ArquiteturaRegras.pacoteBase(FIXTURES + ".forapacote.base"), classes, "forapacote.outro.ClasseIntrusa");
		}

		@Test
		void classeNaCamadaCorreta() {
			conforme("camadacorreta", ArquiteturaRegras::classeEmCamada);
		}

		@Test
		void classeSoltaNaRaizDaFeature() {
			violacao("soltanafeature", ArquiteturaRegras::classeEmCamada, "produto.ProdutoUtil");
		}

		@Test
		void classeSoltaNaRaizDoPacoteBase() {
			violacao("soltanaraiz", ArquiteturaRegras::raizSomenteComAplicacao, "Utilitario");
		}

	}

	@Nested
	class DirecaoDasCamadas {

		@Test
		void serviceDependeDeController() {
			violacao("servicecontroller", ArquiteturaRegras::direcaoDasCamadas, "produto.service.ProdutoService");
		}

		@Test
		void repositoryDependeDeService() {
			violacao("repositoryservice", ArquiteturaRegras::direcaoDasCamadas, "produto.repository.ProdutoRepository");
		}

		@Test
		void modelDependeDeDto() {
			violacao("modeldto", ArquiteturaRegras::direcaoDasCamadas, "produto.model.Produto");
		}

		@Test
		void dtoDependeDeModel() {
			violacao("dtomodel", ArquiteturaRegras::direcaoDasCamadas, "produto.dto.ProdutoResponse");
		}

		@Test
		void dependenciasNaDirecaoPermitidaRespeitamTodasAsRegras() {
			todasConformes("direcaopermitida");
		}

	}

	@Nested
	class CamadaFactory {

		@Test
		void factoryConverteEntreDtoEModel() {
			todasConformes("factoryconverte");
		}

		@Test
		void controllerUsaFactory() {
			violacao("controllerfactory", ArquiteturaRegras::direcaoDasCamadas, "produto.controller.ProdutoController");
		}

		@Test
		void factoryDependeDeRepository() {
			violacao("factoryrepository", ArquiteturaRegras::direcaoDasCamadas, "produto.factory.ProdutoFactory");
		}

		@Test
		void factoryDependeDeService() {
			violacao("factoryservice", ArquiteturaRegras::direcaoDasCamadas, "produto.factory.ProdutoFactory");
		}

	}

	@Nested
	class ControllerSemPersistencia {

		@Test
		void controllerAcessaRepository() {
			violacao("controllerrepository", ArquiteturaRegras::controllerSemPersistencia, "produto.controller.ProdutoController");
		}

		@Test
		void controllerExpoeModel() {
			violacao("controllermodel", ArquiteturaRegras::controllerSemPersistencia, "produto.controller.ProdutoController");
		}

	}

	@Nested
	class IsolamentoEntreFeatures {

		@Test
		void acessoAoRepositoryDeOutraFeature() {
			violacao("repooutrafeature", ArquiteturaRegras::isolamentoEntreFeatures, "estoque.service.EstoqueService");
		}

		@Test
		void acessoAoServiceDeOutraFeature() {
			todasConformes("serviceoutrafeature");
		}

		@Test
		void cicloEntreFeatures() {
			violacao("ciclo", ArquiteturaRegras::featuresSemCiclos, "estoque.service.EstoqueService");
		}

	}

	@Nested
	class Shared {

		@Test
		void sharedDependeDeFeature() {
			violacao("shareddependefeature", ArquiteturaRegras::sharedIndependente, "shared.Formatador");
		}

		@Test
		void featureUsaShared() {
			todasConformes("featureusashared");
		}

	}

	@Nested
	class ConvencoesEInjecao {

		@Test
		void controllerSemSufixo() {
			violacao("controllersemsufixo", raiz -> ArquiteturaRegras.sufixo(raiz, "controller", "Controller"),
					"produto.controller.ProdutoEndpoint");
		}

		@Test
		void factorySemSufixo() {
			violacao("factorysemsufixo", raiz -> ArquiteturaRegras.sufixo(raiz, "factory", "Factory"),
					"produto.factory.ProdutoMapper");
		}

		@Test
		void endpointForaDaCamadaController() {
			violacao("endpointforacontroller", ArquiteturaRegras::endpointNaCamadaController,
					"produto.service.ProdutoService");
		}

		@Test
		void injecaoEmAtributo() {
			violacao("injecaoatributo", raiz -> ArquiteturaRegras.injecaoPorConstrutor(),
					"produto.service.ProdutoService");
		}

	}

	@Nested
	class PontoFlutuante {

		@Test
		void atributoDoubleEmModel() {
			violacao("doublemodel", ArquiteturaRegras::semPontoFlutuante, "produto.model.Produto.preco");
		}

		@Test
		void parametroFloatEmService() {
			violacao("floatservice", ArquiteturaRegras::semPontoFlutuante, "produto.service.ProdutoService.aplicarDesconto");
		}

	}

	private static JavaClasses importar(String caso) {
		return new ClassFileImporter().importPackages(FIXTURES + "." + caso);
	}

	/** Verifica que a regra reprova o caso e que a mensagem nomeia a regra e o infrator. */
	private static void violacao(String caso, Function<String, ArchRule> regra, String infrator) {
		violacao(regra.apply(FIXTURES + "." + caso), importar(caso), caso + "." + infrator);
	}

	private static void violacao(ArchRule regra, JavaClasses classes, String infrator) {
		AssertionError erro = assertThrows(AssertionError.class, () -> regra.check(classes));
		assertThat(erro.getMessage())
			.contains(regra.getDescription())
			.contains(FIXTURES + "." + infrator);
	}

	private static void conforme(String caso, Function<String, ArchRule> regra) {
		JavaClasses classes = importar(caso);
		assertThat(classes).isNotEmpty();
		assertThatCode(() -> regra.apply(FIXTURES + "." + caso).check(classes)).doesNotThrowAnyException();
	}

	private static void todasConformes(String caso) {
		JavaClasses classes = importar(caso);
		assertThat(classes).isNotEmpty();
		ArquiteturaRegras.todas(FIXTURES + "." + caso)
			.forEach((nome, regra) -> assertThatCode(() -> regra.check(classes))
				.as("regra '%s' no caso %s", nome, caso)
				.doesNotThrowAnyException());
	}

}
