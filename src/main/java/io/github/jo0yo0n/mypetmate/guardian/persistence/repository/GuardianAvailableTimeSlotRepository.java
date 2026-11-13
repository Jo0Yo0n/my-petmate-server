package io.github.jo0yo0n.mypetmate.guardian.persistence.repository;

import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianAvailableTimeSlot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianAvailableTimeSlotRepository
    extends JpaRepository<GuardianAvailableTimeSlot, UUID> {

  List<GuardianAvailableTimeSlot> findAllByGuardianId(UUID guardianId);
}
