package io.github.jo0yo0n.mypetmate.guardian.persistence;

import io.github.jo0yo0n.mypetmate.error.StateConflictException;
import io.github.jo0yo0n.mypetmate.guardian.domain.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guardian")
public class Guardian {

  @Id private UUID id;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "profile_type", nullable = false, length = 16)
  private ProfileType profileType;

  @Column(length = 8)
  private Gender gender;

  @Column(name = "discovery_status", nullable = false, length = 16)
  private DiscoveryStatus discoveryStatus;

  @Column(nullable = false, length = 24)
  private GuardianStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Guardian() {}

  public Guardian(
      UUID id,
      String email,
      String passwordHash,
      ProfileType profileType,
      Gender gender,
      DiscoveryStatus discoveryStatus,
      GuardianStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.profileType = profileType;
    this.gender = gender;
    this.discoveryStatus = discoveryStatus;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public void updateProfile(
      ProfileType profileType,
      Gender requestedGender,
      DiscoveryStatus discoveryStatus,
      Instant now) {
    if (profileType == ProfileType.INDIVIDUAL) {
      if (gender != null && gender != requestedGender) {
        throw new StateConflictException("최초 설정한 성별은 변경할 수 없습니다.");
      }
      gender = requestedGender;
    }
    this.profileType = profileType;
    this.discoveryStatus = discoveryStatus;
    updatedAt = now;
  }

  public void withdraw(Instant now) {
    status = GuardianStatus.WITHDRAWN;
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public ProfileType getProfileType() {
    return profileType;
  }

  public Gender getGender() {
    return gender;
  }

  public DiscoveryStatus getDiscoveryStatus() {
    return discoveryStatus;
  }

  public GuardianStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
