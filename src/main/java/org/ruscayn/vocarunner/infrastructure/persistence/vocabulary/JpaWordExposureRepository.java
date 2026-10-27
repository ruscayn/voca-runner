package org.ruscayn.vocarunner.infrastructure.persistence.vocabulary;

import org.ruscayn.vocarunner.domain.model.vocabulary.WordExposure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaWordExposureRepository extends JpaRepository<WordExposure, Long> {
}
