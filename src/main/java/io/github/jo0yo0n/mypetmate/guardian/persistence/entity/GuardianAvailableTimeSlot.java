package io.github.jo0yo0n.mypetmate.guardian.persistence.entity;

import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "guardian_available_time_slot")
public class GuardianAvailableTimeSlot {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "guardian_id", nullable = false)
  private Guardian guardian;

  @Column(name = "day_of_week", nullable = false, length = 3)
  private Weekday dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  protected GuardianAvailableTimeSlot() {}

  public GuardianAvailableTimeSlot(
      UUID id, Guardian guardian, Weekday dayOfWeek, LocalTime startTime, LocalTime endTime) {
    this.id = id;
    this.guardian = guardian;
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public UUID getId() {
    return id;
  }

  public Guardian getGuardian() {
    return guardian;
  }

  public Weekday getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }
}
