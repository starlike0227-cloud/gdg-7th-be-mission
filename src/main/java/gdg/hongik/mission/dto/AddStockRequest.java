package gdg.hongik.mission.dto;

import jakarta.validation.constraints.*;

public record AddStockRequest(
        @NotBlank(message = "상품명을 입력해주세요.") String itemName,
        @NotNull(message = "추가 수량을 입력해주세요.")
        @Positive(message = "추가 수량은 1 이상이어야 합니다.") Integer quantity
) {}
