package br.com.valorizamei.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;

/**
 * Regras de arquitetura do projeto, parametrizadas pelo pacote raiz.
 * <p>
 * O mesmo conjunto roda no código de produção ({@link ArquiteturaTest}) e nas fixtures
 * de {@code arquitetura.fixtures} ({@link ArquiteturaRegrasTest}), que provam que cada
 * regra reprova o caso de violação.
 */
final class ArquiteturaRegras {

	static final String SHARED = "shared";

	private static final List<String> CAMADAS_EXPOSTAS_A_OUTRAS_FEATURES = List.of("service", "dto");

	private static final Set<String> TIPOS_PONTO_FLUTUANTE = Set.of(
			"double", "float", Double.class.getName(), Float.class.getName());

	private ArquiteturaRegras() {
	}

	static Map<String, ArchRule> todas(String raiz) {
		Map<String, ArchRule> regras = new LinkedHashMap<>();
		regras.put("pacote base", pacoteBase(raiz));
		regras.put("classe em camada", classeEmCamada(raiz));
		regras.put("raiz só com a aplicação", raizSomenteComAplicacao(raiz));
		regras.put("direção das camadas", direcaoDasCamadas(raiz));
		regras.put("controller sem persistência", controllerSemPersistencia(raiz));
		regras.put("isolamento entre features", isolamentoEntreFeatures(raiz));
		regras.put("features sem ciclos", featuresSemCiclos(raiz));
		regras.put("shared independente", sharedIndependente(raiz));
		regras.put("sufixo Controller", sufixo(raiz, "controller", "Controller"));
		regras.put("sufixo Service", sufixo(raiz, "service", "Service"));
		regras.put("sufixo Repository", sufixo(raiz, "repository", "Repository"));
		regras.put("sufixo Factory", sufixo(raiz, "factory", "Factory"));
		regras.put("endpoint na camada controller", endpointNaCamadaController(raiz));
		regras.put("injeção por construtor", injecaoPorConstrutor());
		regras.put("sem ponto flutuante", semPontoFlutuante(raiz));
		return regras;
	}

	static ArchRule pacoteBase(String raiz) {
		return classes()
			.should().resideInAPackage(raiz + "..")
			.because("todo código deve ficar sob o pacote base " + raiz)
			.allowEmptyShould(true);
	}

	static ArchRule classeEmCamada(String raiz) {
		return classes()
			.that().resideInAPackage(raiz + ".*..")
			.and().resideOutsideOfPackage(raiz + "." + SHARED + "..")
			.should().resideInAnyPackage(
					camada(raiz, "controller"), camada(raiz, "service"), camada(raiz, "repository"),
					camada(raiz, "model"), camada(raiz, "dto"), camada(raiz, "factory"))
			.because("toda classe de feature deve ficar em uma camada: controller, service, repository, model, dto ou factory")
			.allowEmptyShould(true);
	}

	static ArchRule raizSomenteComAplicacao(String raiz) {
		return classes()
			.that().resideInAPackage(raiz)
			.should().beAnnotatedWith(SpringBootApplication.class)
			.because("a raiz do pacote base só pode conter a classe de inicialização da aplicação")
			.allowEmptyShould(true);
	}

	static ArchRule direcaoDasCamadas(String raiz) {
		return layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.withOptionalLayers(true)
			.layer("Controller").definedBy(camada(raiz, "controller"))
			.layer("Service").definedBy(camada(raiz, "service"))
			.layer("Factory").definedBy(camada(raiz, "factory"))
			.layer("Repository").definedBy(camada(raiz, "repository"))
			.layer("Model").definedBy(camada(raiz, "model"))
			.layer("Dto").definedBy(camada(raiz, "dto"))
			.whereLayer("Controller").mayNotBeAccessedByAnyLayer()
			.whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
			.whereLayer("Factory").mayOnlyBeAccessedByLayers("Service")
			.whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
			.whereLayer("Dto").mayOnlyBeAccessedByLayers("Controller", "Service", "Factory")
			.whereLayer("Model").mayOnlyBeAccessedByLayers("Service", "Factory", "Repository")
			.because("as dependências seguem controller -> service -> repository -> model; a conversão dto/model fica na factory")
			.allowEmptyShould(true);
	}

	static ArchRule controllerSemPersistencia(String raiz) {
		return noClasses()
			.that().resideInAPackage(camada(raiz, "controller"))
			.should().dependOnClassesThat().resideInAnyPackage(camada(raiz, "repository"), camada(raiz, "model"))
			.because("o controller recebe e devolve dto e acessa dados somente por um service")
			.allowEmptyShould(true);
	}

	static ArchRule isolamentoEntreFeatures(String raiz) {
		return classes()
			.that().resideInAPackage(raiz + ".*..")
			.should(acessarOutrasFeaturesSomentePor(raiz, CAMADAS_EXPOSTAS_A_OUTRAS_FEATURES))
			.because("uma feature só acessa outra pelas camadas service e dto dela")
			.allowEmptyShould(true);
	}

	static ArchRule featuresSemCiclos(String raiz) {
		return slices()
			.matching(raiz + ".(*)..")
			.should().beFreeOfCycles()
			.because("dependências entre features não podem formar ciclos")
			.allowEmptyShould(true);
	}

	static ArchRule sharedIndependente(String raiz) {
		return classes()
			.that().resideInAPackage(raiz + "." + SHARED + "..")
			.should(naoDependerDeFeatures(raiz))
			.because("o código transversal em shared não pode depender de nenhuma feature")
			.allowEmptyShould(true);
	}

	static ArchRule sufixo(String raiz, String nomeCamada, String sufixo) {
		return classes()
			.that().resideInAPackage(camada(raiz, nomeCamada))
			.and().arePublic()
			.and().areTopLevelClasses()
			.should().haveSimpleNameEndingWith(sufixo)
			.because("classes públicas da camada " + nomeCamada + " terminam com " + sufixo)
			.allowEmptyShould(true);
	}

	static ArchRule endpointNaCamadaController(String raiz) {
		return classes()
			.that().areMetaAnnotatedWith(Controller.class)
			.should().resideInAPackage(camada(raiz, "controller"))
			.because("classes que expõem endpoints HTTP ficam na camada controller")
			.allowEmptyShould(true);
	}

	static ArchRule injecaoPorConstrutor() {
		return GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
			.because("dependências são injetadas pelo construtor")
			.allowEmptyShould(true);
	}

	static ArchRule semPontoFlutuante(String raiz) {
		return classes()
			.that().resideInAPackage(raiz + ".*..")
			.and().resideOutsideOfPackage(raiz + "." + SHARED + "..")
			.should(naoUsarPontoFlutuante())
			.because("dinheiro e percentuais usam BigDecimal, nunca double ou float")
			.allowEmptyShould(true);
	}

	private static String camada(String raiz, String nomeCamada) {
		return raiz + ".*." + nomeCamada + "..";
	}

	/** Nome da feature de uma classe, ou {@code null} se ela não estiver em uma feature. */
	private static String featureDe(JavaClass classe, String raiz) {
		String segmento = segmento(classe, raiz, 0);
		return SHARED.equals(segmento) ? null : segmento;
	}

	/** Segmento do pacote logo após a raiz, deslocado por {@code indice}, ou {@code null}. */
	private static String segmento(JavaClass classe, String raiz, int indice) {
		String pacote = classe.getPackageName();
		if (!pacote.startsWith(raiz + ".")) {
			return null;
		}
		String[] segmentos = pacote.substring(raiz.length() + 1).split("\\.");
		return indice < segmentos.length ? segmentos[indice] : null;
	}

	private static ArchCondition<JavaClass> acessarOutrasFeaturesSomentePor(String raiz, List<String> camadasPermitidas) {
		return new ArchCondition<>("acessar outras features somente pelas camadas " + camadasPermitidas) {
			@Override
			public void check(JavaClass origem, ConditionEvents eventos) {
				String featureOrigem = featureDe(origem, raiz);
				if (featureOrigem == null) {
					return;
				}
				for (Dependency dependencia : origem.getDirectDependenciesFromSelf()) {
					JavaClass destino = dependencia.getTargetClass().getBaseComponentType();
					String featureDestino = featureDe(destino, raiz);
					boolean outraFeature = featureDestino != null && !featureDestino.equals(featureOrigem);
					if (outraFeature && !camadasPermitidas.contains(segmento(destino, raiz, 1))) {
						eventos.add(SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
					}
				}
			}
		};
	}

	private static ArchCondition<JavaClass> naoDependerDeFeatures(String raiz) {
		return new ArchCondition<>("não depender de features") {
			@Override
			public void check(JavaClass origem, ConditionEvents eventos) {
				for (Dependency dependencia : origem.getDirectDependenciesFromSelf()) {
					if (featureDe(dependencia.getTargetClass().getBaseComponentType(), raiz) != null) {
						eventos.add(SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
					}
				}
			}
		};
	}

	private static ArchCondition<JavaClass> naoUsarPontoFlutuante() {
		return new ArchCondition<>("não usar double, float, Double ou Float") {
			@Override
			public void check(JavaClass classe, ConditionEvents eventos) {
				for (JavaField atributo : classe.getFields()) {
					if (ehPontoFlutuante(atributo.getRawType())) {
						eventos.add(SimpleConditionEvent.violated(atributo,
								"Atributo " + atributo.getFullName() + " é do tipo " + atributo.getRawType().getName()));
					}
				}
				for (JavaCodeUnit metodo : classe.getCodeUnits()) {
					if (metodo.getModifiers().contains(JavaModifier.SYNTHETIC)) {
						continue;
					}
					Stream.concat(metodo.getRawParameterTypes().stream(), Stream.of(metodo.getRawReturnType()))
						.filter(ArquiteturaRegras::ehPontoFlutuante)
						.findFirst()
						.ifPresent(tipo -> eventos.add(SimpleConditionEvent.violated(metodo,
								"Método " + metodo.getFullName() + " usa o tipo " + tipo.getName())));
				}
			}
		};
	}

	private static boolean ehPontoFlutuante(JavaClass tipo) {
		return TIPOS_PONTO_FLUTUANTE.contains(tipo.getBaseComponentType().getName());
	}

}
