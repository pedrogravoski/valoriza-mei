package br.com.valorizamei.arquitetura;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Aplica as regras de arquitetura ao código de produção. Importa todo o classpath de
 * produção (sem jars e sem testes) para também detectar classes fora do pacote base.
 */
class ArquiteturaTest {

	private static final String RAIZ = "br.com.valorizamei";

	private static final JavaClasses CLASSES_DE_PRODUCAO = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES)
		.importClasspath();

	@TestFactory
	Stream<DynamicTest> codigoDeProducaoRespeitaAsRegras() {
		return ArquiteturaRegras.todas(RAIZ).entrySet().stream()
			.map(regra -> dynamicTest(regra.getKey(), () -> regra.getValue().check(CLASSES_DE_PRODUCAO)));
	}

}
