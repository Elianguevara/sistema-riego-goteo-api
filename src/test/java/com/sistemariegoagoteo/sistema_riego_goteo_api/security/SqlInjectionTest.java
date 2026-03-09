package com.sistemariegoagoteo.sistema_riego_goteo_api.security;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Pruebas de Inyección SQL (Caja Negra - Seguridad de Persistencia).
 *
 * Nivel cubierto: Integración con capa JPA/H2.
 *
 * OBJETIVO: Demostrar que Spring Data JPA usa queries parametrizadas (JPQL/HQL
 * con parámetros con nombre) que NEUTRALIZAN automáticamente los intentos de
 * inyección SQL. El ORM trata el input malicioso como un valor literal, no como
 * código SQL ejecutable.
 *
 * Metodología:
 * - Se insertan payloads de inyección SQL clásicos como valores de búsqueda.
 * - Se verifica que la query NO lanza excepción (no se ejecuta SQL inyectado).
 * - Se verifica que retorna vacío (el valor inyectado no coincide con datos
 * reales).
 * - Se verifica que los datos legítimos pre-existentes NO fueron destruidos.
 */
@DataJpaTest
// Usar la config H2 de application-test.properties (incluye NON_KEYWORDS=USER)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("SQL Injection - Tests de Neutralización por JPA Parametrizado")
class SqlInjectionTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private FarmRepository farmRepository;

    private Farm fincaLegitima;

    @BeforeEach
    void setUp() {
        // Datos legítimos que deben sobrevivir a cualquier ataque de inyección
        Role role = new Role("ROLE_SQL_TEST");
        em.persist(role);

        fincaLegitima = new Farm();
        fincaLegitima.setName("Finca Legitima SQL Test");
        fincaLegitima.setLocation("Mendoza, Argentina");
        fincaLegitima.setReservoirCapacity(new BigDecimal("200.00"));
        fincaLegitima.setFarmSize(new BigDecimal("15.00"));
        fincaLegitima = em.persist(fincaLegitima);

        em.flush();
    }

    // =========================================================================
    // GRUPO 1: Inyección en findByName() - parámetro de nombre de finca
    // =========================================================================

    @Test
    @DisplayName("findByName() con payload DROP TABLE no ejecuta SQL: retorna vacío sin excepción")
    void findByName_payloadDropTable_noEjecuta_retornaVacio() {
        // Payload clásico de inyección SQL
        String payloadDropTable = "Finca'; DROP TABLE farm; --";

        // Spring Data JPA pasa esto como parámetro enlazado, no como SQL raw
        assertThatNoException().isThrownBy(() -> {
            Optional<Farm> result = farmRepository.findByName(payloadDropTable);
            // El payload es tratado literalmente como string, no como SQL
            assertThat(result).isEmpty();
        });

        // La tabla sigue intacta: la finca legítima aún existe
        assertThat(farmRepository.findById(fincaLegitima.getId())).isPresent();
    }

    @Test
    @DisplayName("findByName() con payload OR 1=1 no retorna todos los registros")
    void findByName_payloadOr1Igual1_noRetornaTodosLosRegistros() {
        // Este payload en SQL raw retornaría TODOS los registros: WHERE name = '' OR
        // 1=1
        String payloadOr = "' OR '1'='1";

        Optional<Farm> result = farmRepository.findByName(payloadOr);

        // JPA lo trata como string literal → no coincide con ningún nombre real
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByName() con payload UNION SELECT no expone datos de otras tablas")
    void findByName_payloadUnionSelect_noExponeOtrasTablas() {
        String payloadUnion = "' UNION SELECT username, password, email, 1, 1, 1, 1, 1, 1 FROM user --";

        // Debe retornar vacío sin lanzar excepción: el UNION no se ejecuta
        Optional<Farm> result = farmRepository.findByName(payloadUnion);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByName() con payload de comentario SQL (--) no altera la query")
    void findByName_payloadComentarioSQL_noAlteraQuery() {
        String payloadComment = "Finca Legitima SQL Test'--";

        // No debe coincidir aunque empiece igual que la finca real
        Optional<Farm> result = farmRepository.findByName(payloadComment);

        assertThat(result).isEmpty();
        // La finca legítima sigue existiendo bajo su nombre real
        assertThat(farmRepository.findByName("Finca Legitima SQL Test")).isPresent();
    }

    @Test
    @DisplayName("findByName() con payload de stacked queries (;) no ejecuta segunda sentencia")
    void findByName_payloadStackedQuery_noEjecutaSegundaSentencia() {
        String payloadStacked = "test'; INSERT INTO farm (name, reservoir_capacity, farm_size) VALUES ('HACK', 0, 0); --";

        assertThatNoException().isThrownBy(() -> farmRepository.findByName(payloadStacked));

        // No se insertó ninguna finca extra con nombre 'HACK'
        assertThat(farmRepository.findByName("HACK")).isEmpty();
    }

    // =========================================================================
    // GRUPO 2: Inyección en findFarmsByUsername() - query JPQL con @Param
    // =========================================================================

    @Test
    @DisplayName("findFarmsByUsername() con payload OR 1=1 en username no expone todas las fincas")
    void findFarmsByUsername_payloadOr1Igual1_noRetornaTodo() {
        // En SQL vulnerable: WHERE u.username = '' OR '1'='1' → retornaría todo
        String payloadUsername = "' OR '1'='1";

        List<Farm> result = farmRepository.findFarmsByUsername(payloadUsername);

        // Con JPQL parametrizado, el payload es string literal: ningún user tiene ese
        // username
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findFarmsByUsername() con payload DROP retorna vacío sin destruir datos")
    void findFarmsByUsername_payloadDrop_noDestruyeDatos() {
        String payloadDrop = "admin'; DROP TABLE user; --";

        assertThatNoException().isThrownBy(() -> {
            List<Farm> result = farmRepository.findFarmsByUsername(payloadDrop);
            assertThat(result).isEmpty();
        });

        // Los datos siguen intactos
        assertThat(farmRepository.findAll()).isNotEmpty();
    }

    // =========================================================================
    // GRUPO 3: Verificación positiva — búsquedas legítimas siguen funcionando
    // =========================================================================

    @Test
    @DisplayName("findByName() con nombre legítimo retorna correctamente después de intentos de inyección")
    void findByName_nombreLegitimo_despuesDeInyeccion_retornaCorrectamente() {
        // Simular que primero se intentaron varias inyecciones
        farmRepository.findByName("'; DROP TABLE farm; --");
        farmRepository.findByName("' OR '1'='1");
        farmRepository.findByName("' UNION SELECT * FROM user --");

        // Ahora una búsqueda legítima debe seguir funcionando
        Optional<Farm> result = farmRepository.findByName("Finca Legitima SQL Test");

        assertThat(result).isPresent();
        assertThat(result.get().getLocation()).isEqualTo("Mendoza, Argentina");
    }

    @Test
    @DisplayName("Nombre de finca con caracteres especiales legítimos se guarda y recupera correctamente")
    void farmConCaracteresEspeciales_sePersistYRecupera() {
        // Nombres con apóstrofes o caracteres especiales legítimos (ej. nombres de
        // lugares)
        Farm farmConApostrofe = new Farm();
        farmConApostrofe.setName("L'Estrel·la Farm");
        farmConApostrofe.setLocation("Cataluña, España");
        farmConApostrofe.setReservoirCapacity(new BigDecimal("100.0"));
        farmConApostrofe.setFarmSize(new BigDecimal("8.0"));

        // JPA maneja el apóstrofe de forma segura como valor parametrizado
        Farm saved = farmRepository.save(farmConApostrofe);
        em.flush();
        em.clear();

        Optional<Farm> found = farmRepository.findByName("L'Estrel·la Farm");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }
}
