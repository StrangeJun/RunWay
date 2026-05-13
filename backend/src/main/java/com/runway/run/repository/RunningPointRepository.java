package com.runway.run.repository;

import com.runway.run.domain.RunningPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RunningPointRepository extends JpaRepository<RunningPoint, UUID> {

    List<RunningPoint> findByRunningRecordIdOrderBySequenceAsc(UUID runningRecordId);

    void deleteByRunningRecordId(UUID runningRecordId);
}
