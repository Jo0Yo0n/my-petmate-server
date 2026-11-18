package io.github.jo0yo0n.mypetmate.guardian.persistence.entity;

import io.github.jo0yo0n.mypetmate.guardian.domain.Weekday;
import jakarta.persistence.*;
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

  @Column(name = "start_minute", nullable = false)
  private int startMinute;

  @Column(name = "end_minute", nullable = false)
  private int endMinute;

  protected GuardianAvailableTimeSlot() {}

  public GuardianAvailableTimeSlot(
      UUID id, Guardian guardian, Weekday dayOfWeek, int startMinute, int endMinute) {
    this.id = id;
    this.guardian = guardian;
    this.dayOfWeek = dayOfWeek;
    this.startMinute = startMinute;
    this.endMinute = endMinute;
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

  public int getStartMinute() {
    return startMinute;
  }

  public int getEndMinute() {
    return endMinute;
  }
}
