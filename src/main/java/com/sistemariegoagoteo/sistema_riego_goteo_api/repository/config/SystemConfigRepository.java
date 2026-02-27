package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.config;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.config.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    List<SystemConfig> findByConfigKeyIn(List<String> keys);
}
