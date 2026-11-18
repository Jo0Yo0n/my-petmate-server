package io.github.jo0yo0n.mypetmate.guardian.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.jo0yo0n.mypetmate.guardian.domain.DiscoveryStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.validation.activity.GuardianActivityFields;
import io.github.jo0yo0n.mypetmate.guardian.validation.activity.NoOverlappingTimeSlots;
import io.github.jo0yo0n.mypetmate.guardian.validation.activity.RequiresActivityFieldsWhenSearching;
import io.github.jo0yo0n.mypetmate.guardian.validation.profile.GuardianProfileFields;
import io.github.jo0yo0n.mypetmate.guardian.validation.profile.ValidGuardianProfile;
import io.github.jo0yo0n.mypetmate.web.json.JsonStringDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.UniqueElements;

@ValidGuardianProfile
@RequiresActivityFieldsWhenSearching
@NoOverlappingTimeSlots
public record GuardianUpdateRequest(
    @NotBlank(message = "필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        @Pattern(regexp = "[a-zA-Z가-힣0-9]+", message = "닉네임은 영문, 한글, 숫자만 사용할 수 있습니다.")
        @JsonDeserialize(using = JsonStringDeserializer.class)
        String nickname,
    @NotNull(message = "필수입니다.") ProfileType profileType,
    Gender gender,
    @NotNull(message = "필수입니다.") DiscoveryStatus discoveryStatus,
    @Size(max = 3, message = "주 활동 지역은 최대 3개까지 선택할 수 있습니다.")
        @UniqueElements(message = "같은 활동 지역을 중복으로 선택할 수 없습니다.")
        List<@NotNull(message = "필수입니다.") UUID> activityRegionIds,
    @Size(max = 21, message = "활동 가능 시간대는 최대 {max}개까지 등록할 수 있습니다.")
        List<@NotNull(message = "필수입니다.") @Valid AvailableTimeSlotRequest> availableTimeSlots)
    implements GuardianProfileFields, GuardianActivityFields {}
