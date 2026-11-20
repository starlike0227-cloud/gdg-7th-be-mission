package gdg.hongik.mission.controller;

import gdg.hongik.mission.dto.AddStockRequest;
import gdg.hongik.mission.dto.ProductRequest;
import gdg.hongik.mission.dto.ProductResponse;
import gdg.hongik.mission.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/products")
@Tag(name = "관리자 상품 API", description = "상품 등록, 재고 추가, 삭제")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;

    @Operation(summary = "상품 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @Operation(summary = "상품명으로 재고 추가")
    @PatchMapping("/stock")
    public ProductResponse addStock(@Valid @RequestBody AddStockRequest request) {
        return productService.addStock(request.itemName(), request.quantity());
    }

    @Operation(summary = "선택한 상품 삭제", description = "삭제할 상품 ID만 입력하세요. 삭제 후 남은 상품을 반환합니다.")
    @DeleteMapping
    public List<ProductResponse> deleteProducts(
            @Parameter(
                    name = "ids",
                    in = ParameterIn.QUERY,
                    description = "삭제할 상품 ID. 상품 하나만 삭제하려면 ID 하나만 입력하세요.",
                    required = true,
                    array = @ArraySchema(schema = @Schema(type = "integer", format = "int64"))
            )
            @RequestParam("ids") @NotEmpty(message = "삭제할 상품 ID를 입력해주세요.")
            List<@NotNull @Positive(message = "상품 ID는 1 이상이어야 합니다.") Long> ids
    ) {
        return productService.deleteProducts(ids);
    }
}
