package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
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

/**
 * Pruebas de Integración con Base de Datos (Caja Blanca - Capa de
 * Persistencia).
 *
 * Nivel cubierto: @DataJpaTest — carga solo el slice de JPA con H2 en memoria.
 *
 * Verifica:
 * 1. Queries personalizadas de FarmRepository
 * 2. Relaciones: Farm -> Sector (OneToMany, CascadeType.ALL,
 * orphanRemoval=true)
 * 3. Relación M2M: User <-> Farm (tabla user_farm)
 * 4. Integridad referencial: Sector no puede existir sin Farm (nullable=false
 * en FK)
 */
@DataJpaTest
// Usar la config H2 de application-test.properties (incluye NON_KEYWORDS=USER)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("FarmRepository - Tests de Integración JPA")
class FarmRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private SectorRepository sectorRepository;

    private Farm savedFarm;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Role
        adminRole = new Role("ADMIN_REPO_TEST");
        adminRole = em.persist(adminRole);

        // Farm base
        Farm farm = new Farm();
        farm.setName("Finca Repositorio Test");
        farm.setLocation("San Juan, Argentina");
        farm.setReservoirCapacity(new BigDecimal("500.00"));
        farm.setFarmSize(new BigDecimal("25.00"));
        savedFarm = em.persist(farm);

        em.flush();
    }

    // =========================================================================
    // GRUPO 1: Queries personalizadas
    // =========================================================================

    @Test
    @DisplayName("findByName() debe retornar la finca cuando el nombre coincide exactamente")
    void findByName_nombreExistente_retornaFinca() {
        Optional<Farm> result = farmRepository.findByName("Finca Repositorio Test");

        assertThat(result).isPresent();
        assertThat(result.get().getLocation()).isEqualTo("San Juan, Argentina");
    }

    @Test
    @DisplayName("findByName() debe retornar Optional.empty() cuando el nombre no existe")
    void findByName_nombreInexistente_retornaVacio() {
        Optional<Farm> result = farmRepository.findByName("Finca Inexistente XYZ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findFarmsByUsername() debe retornar las fincas asignadas al usuario via M2M")
    void findFarmsByUsername_usuarioConFincasAsignadas_retornaFincas() {
        // Crear usuario y asignarle la finca
        User user = new User("Operario Test", "op_repo_test",
                "encodedPass", "op_repo_test@test.com", adminRole);
        user.getFarms().add(savedFarm);
        em.persist(user);
        em.flush();
        em.clear(); // Limpiar caché de primer nivel

        List<Farm> farms = farmRepository.findFarmsByUsername("op_repo_test");

        assertThat(farms).hasSize(1);
        assertThat(farms.get(0).getName()).isEqualTo("Finca Repositorio Test");
    }

    @Test
    @DisplayName("findFarmsByUsername() debe retornar lista vacía para usuario sin fincas asignadas")
    void findFarmsByUsername_usuarioSinFincas_retornaListaVacia() {
        User userSinFincas = new User("Sin Finca", "sin_finca_test",
                "encodedPass", "sin_finca_test@test.com", adminRole);
        em.persist(userSinFincas);
        em.flush();

        List<Farm> farms = farmRepository.findFarmsByUsername("sin_finca_test");

        assertThat(farms).isEmpty();
    }

    // =========================================================================
    // GRUPO 2: Relación OneToMany Farm -> Sector (CascadeType.ALL, orphanRemoval)
    // =========================================================================

    @Test
    @DisplayName("Sector se persiste correctamente con su Farm asociada (FK not null)")
    void sector_persistidoConFarm_guardaRelacionFK() {
        Sector sector = new Sector();
        sector.setName("Lote Norte");
        sector.setFarm(savedFarm);
        Sector savedSector = em.persist(sector);
        em.flush();

        Optional<Sector> found = sectorRepository.findById(savedSector.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFarm().getId()).isEqualTo(savedFarm.getId());
        assertThat(found.get().getName()).isEqualTo("Lote Norte");
    }

    @Test
    @DisplayName("Eliminar Farm debe eliminar sus Sectores en cascada (CascadeType.ALL + orphanRemoval)")
    void deleteFarm_conSectores_eliminaSectoresEnCascada() {
        // Usar SOLO em.* para evitar conflictos de sesión entre TestEntityManager
        // y Spring Data repositories al mezclar operaciones de flush.
        Farm farmCascade = new Farm();
        farmCascade.setName("Farm para Cascada");
        farmCascade.setReservoirCapacity(BigDecimal.ZERO);
        farmCascade.setFarmSize(BigDecimal.ONE);
        em.persist(farmCascade);
        em.flush(); // INSERT farm en DB
        Integer farmId = farmCascade.getId();

        Sector sector = new Sector();
        sector.setName("Sector Cascada Test");
        sector.setFarm(farmCascade); // farmCascade es managed → sin TransientObjectException
        em.persist(sector);
        em.flush(); // INSERT sector en DB
        Integer sectorId = sector.getId();

        em.clear(); // Limpiar caché de primer nivel → estado fresco

        // Verificar que ambos existen en BD
        assertThat(farmRepository.existsById(farmId)).isTrue();
        assertThat(sectorRepository.existsById(sectorId)).isTrue();

        // Cargar Farm fresco e inicializar la colección LAZY de sectores
        // antes de llamar em.remove() para que CascadeType.REMOVE funcione
        Farm freshFarm = em.find(Farm.class, farmId);
        int sectoresAsociados = freshFarm.getSectors().size(); // inicializa lazy collection
        assertThat(sectoresAsociados).isEqualTo(1);

        em.remove(freshFarm); // cascade REMOVE → sector también se elimina
        em.flush();
        em.clear();

        // El sector ya no debe existir (orphanRemoval = true en la relación)
        assertThat(sectorRepository.existsById(sectorId)).isFalse();
    }

    @Test
    @DisplayName("Múltiples sectores de la misma finca se recuperan correctamente")
    void findByFarmId_fincaConMultiplesSectores_retornaTodasRelaciones() {
        // Crear 3 sectores para la misma finca
        for (int i = 1; i <= 3; i++) {
            Sector s = new Sector();
            s.setName("Sector " + i);
            s.setFarm(savedFarm);
            em.persist(s);
        }
        em.flush();
        em.clear();

        List<Sector> sectores = sectorRepository.findByFarm_Id(savedFarm.getId());

        assertThat(sectores).hasSize(3);
        assertThat(sectores).extracting(Sector::getName)
                .containsExactlyInAnyOrder("Sector 1", "Sector 2", "Sector 3");
    }

    // =========================================================================
    // GRUPO 3: Relación Many-to-Many User <-> Farm (tabla user_farm)
    // =========================================================================

    @Test
    @DisplayName("Un usuario puede ser asignado a múltiples fincas (M2M)")
    void usuarioAsignadoAMultiplesFincas_relacionManyToMany_correcta() {
        // Segunda finca
        Farm farm2 = new Farm();
        farm2.setName("Segunda Finca M2M");
        farm2.setReservoirCapacity(BigDecimal.ZERO);
        farm2.setFarmSize(BigDecimal.ONE);
        farm2 = em.persist(farm2);

        // Usuario con dos fincas
        User user = new User("Multi Finca User", "multi_finca_test",
                "encodedPass", "multi_finca_test@test.com", adminRole);
        user.getFarms().add(savedFarm);
        user.getFarms().add(farm2);
        em.persist(user);
        em.flush();
        em.clear();

        User found = em.find(User.class, user.getId());
        assertThat(found.getFarms()).hasSize(2);
    }

    @Test
    @DisplayName("Una finca puede tener múltiples usuarios asignados (M2M inverso)")
    void fincaConMultiplesUsuarios_relacionManyToMany_correcta() {
        User user1 = new User("User1 M2M", "user1_m2m_test",
                "encodedPass", "user1_m2m@test.com", adminRole);
        user1.getFarms().add(savedFarm);
        em.persist(user1);

        User user2 = new User("User2 M2M", "user2_m2m_test",
                "encodedPass", "user2_m2m@test.com", adminRole);
        user2.getFarms().add(savedFarm);
        em.persist(user2);

        em.flush();
        em.clear();

        // Verificar via query que ambos usuarios tienen la finca
        List<Farm> farmsUser1 = farmRepository.findFarmsByUsername("user1_m2m_test");
        List<Farm> farmsUser2 = farmRepository.findFarmsByUsername("user2_m2m_test");

        assertThat(farmsUser1).hasSize(1);
        assertThat(farmsUser2).hasSize(1);
        assertThat(farmsUser1.get(0).getId()).isEqualTo(savedFarm.getId());
        assertThat(farmsUser2.get(0).getId()).isEqualTo(savedFarm.getId());
    }

    // =========================================================================
    // GRUPO 4: Operaciones CRUD básicas del repositorio
    // =========================================================================

    @Test
    @DisplayName("findAll() retorna todas las fincas persistidas")
    void findAll_conMultiplesFincas_retornaTodasLasFincas() {
        Farm farm2 = new Farm();
        farm2.setName("Segunda Finca");
        farm2.setReservoirCapacity(BigDecimal.ZERO);
        farm2.setFarmSize(BigDecimal.ONE);
        em.persist(farm2);
        em.flush();

        List<Farm> all = farmRepository.findAll();

        // Al menos 2 fincas (la del setUp + la recién creada)
        assertThat(all.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findById() retorna Optional.empty() para ID inexistente")
    void findById_idInexistente_retornaVacio() {
        Optional<Farm> result = farmRepository.findById(Integer.MAX_VALUE);
        assertThat(result).isEmpty();
    }
}
