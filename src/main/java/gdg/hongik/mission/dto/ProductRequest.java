package gdg.hongik.mission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import gdg.hongik.mission.entity.Category;
import jakarta.validation.constraints.*;

public record ProductRequest(
        @JsonProperty("itemName") @NotBlank(message = "상품명을 입력해주세요.")
        @Size(max = 255, message = "상품명은 255자 이하여야 합니다.") String name,
        @Positive(message = "가격은 1 이상이어야 합니다.") int price,
        @JsonProperty("quantity") @PositiveOrZero(message = "초기 재고는 0 이상이어야 합니다.") int stockQuantity,
        @NotNull(message = "카테고리를 입력해주세요.") Category category
) {}
