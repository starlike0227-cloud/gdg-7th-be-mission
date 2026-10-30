package gdg.hongik.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "상품 정보")
public class Product {

    @Schema(
            description = "서버가 생성하는 상품 ID",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Schema(description = "상품명", example = "keyboard")
    private String name;

    @Schema(description = "개당 가격. 단위는 원", example = "30000")
    private int price;

    @Schema(description = "재고 수량", example = "20")
    private int stockQuantity;
}