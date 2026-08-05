package com.visium.backend.repository;

import com.visium.backend.entity.Recepcionista;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecepcionistaRepository extends JpaRepository<Recepcionista, UUID> {
}