package gdg.hongik.mission.dto;

import jakarta.validation.constraints.*;

public record PurchaseRequest(
        @NotNull(message = "상품 ID를 입력해주세요.")
        @Positive(message = "상품 ID는 1 이상이어야 합니다.") Long id,
        @NotNull(message = "구매 수량을 입력해주세요.")
        @Positive(message = "구매 수량은 1 이상이어야 합니다.") Integer count
) {}
