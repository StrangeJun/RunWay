package com.runway.run.repository;

import com.runway.run.domain.RunningRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RunningRecordRepository extends JpaRepository<RunningRecord, UUID> {

    Optional<RunningRecord> findByIdAndUserId(UUID id, UUID userId);

    Page<RunningRecord> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);
}
