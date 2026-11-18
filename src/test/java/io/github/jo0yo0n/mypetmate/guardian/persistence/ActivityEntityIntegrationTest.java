package io.github.jo0yo0n.mypetmate.guardian.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jo0yo0n.mypetmate.guardian.domain.*;
import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.Guardian;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianActivityRegion;
import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianAvailableTimeSlot;
import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import io.github.jo0yo0n.mypetmate.region.persistence.entity.Region;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ActivityEntityIntegrationTest extends PostgreSqlIntegrationTestSupport {
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void persistsAndRestoresRegionHierarchyAndGuardianActivity() {
    Region sido = new Region(UUID.randomUUID(), "경기도", RegionLevel.SIDO, null);
    Region sgg = new Region(UUID.randomUUID(), "안산시 상록구", RegionLevel.SGG, sido);
    Region umd = new Region(UUID.randomUUID(), "일동", RegionLevel.UMD, sgg);
    entityManager.persist(sido);
    entityManager.persist(sgg);
    entityManager.persist(umd);
    Instant now = Instant.parse("2026-11-10T00:00:00Z");
    Guardian guardian =
        new Guardian(
            UUID.randomUUID(),
            "activity-test",
            "activity@example.com",
            "password-hash",
            ProfileType.INDIVIDUAL,
            Gender.FEMALE,
            DiscoveryStatus.SEARCHING,
            GuardianStatus.ACTIVE,
            now,
            now);
    entityManager.persist(guardian);
    GuardianActivityRegion activity =
        new GuardianActivityRegion(UUID.randomUUID(), guardian, umd);
    entityManager.persist(activity);
    for (Weekday day : Weekday.values()) {
      entityManager.persist(
          new GuardianAvailableTimeSlot(UUID.randomUUID(), guardian, day, 540, 630));
    }
    entityManager.flush();
    entityManager.clear();

    Region restoredUmd = entityManager.find(Region.class, umd.getId());
    assertThat(restoredUmd.getLevel()).isEqualTo(RegionLevel.UMD);
    assertThat(
            entityManager
                .getEntityManagerFactory()
                .getPersistenceUnitUtil()
                .isLoaded(restoredUmd, "parent"))
        .isFalse();
    assertThat(restoredUmd.getParent().getName()).isEqualTo("안산시 상록구");
    assertThat(restoredUmd.getParent().getParent().getLevel()).isEqualTo(RegionLevel.SIDO);
    assertThat(restoredUmd.getParent().getParent().getParent()).isNull();
    GuardianActivityRegion restoredActivity =
        entityManager.find(GuardianActivityRegion.class, activity.getId());
    assertThat(restoredActivity.getGuardian().getId()).isEqualTo(guardian.getId());
    assertThat(restoredActivity.getRegion().getId()).isEqualTo(umd.getId());
    var slots =
        entityManager
            .createQuery(
                "select s from GuardianAvailableTimeSlot s", GuardianAvailableTimeSlot.class)
            .getResultList();
    assertThat(slots)
        .extracting(GuardianAvailableTimeSlot::getDayOfWeek)
        .containsExactlyInAnyOrder(Weekday.values());
    assertThat(slots)
        .allSatisfy(
            slot -> {
              assertThat(slot.getGuardian().getId()).isEqualTo(guardian.getId());
              assertThat(slot.getStartMinute()).isEqualTo(540);
              assertThat(slot.getEndMinute()).isEqualTo(630);
            });
    assertThat(jdbcTemplate.queryForList("select level from region", String.class))
        .containsExactlyInAnyOrder("sido", "sgg", "umd");
    assertThat(
            jdbcTemplate.queryForList(
                "select day_of_week from guardian_available_time_slot", String.class))
        .containsExactlyInAnyOrder("mon", "tue", "wed", "thu", "fri", "sat", "sun");
  }
}
