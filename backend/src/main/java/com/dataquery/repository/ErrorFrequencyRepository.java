package com.dataquery.repository;

import com.dataquery.model.ErrorFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErrorFrequencyRepository extends JpaRepository<ErrorFrequency, Long> {

    // Buscar si ya existe este patrón de error
    Optional<ErrorFrequency> findByErrorPatternAndStatementType(
        String errorPattern, String statementType);

    // Top 10 errores más comunes en todo el sistema (para la IA y el Dashboard)
    List<ErrorFrequency> findTop10ByOrderByOccurrenceCountDesc();

    // Top 5 errores por tipo de sentencia (para sugerencias específicas)
    List<ErrorFrequency> findTop5ByStatementTypeOrderByOccurrenceCountDesc(
        String statementType);
}
