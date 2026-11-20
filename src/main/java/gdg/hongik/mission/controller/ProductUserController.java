package gdg.hongik.mission.controller;

import gdg.hongik.mission.dto.ProductResponse;
import gdg.hongik.mission.dto.PurchaseRequest;
import gdg.hongik.mission.dto.PurchaseResponse;
import gdg.hongik.mission.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/products")
@Tag(name = "소비자 상품 API", description = "상품 조회와 구매")
@RequiredArgsConstructor
public class ProductUserController {

    private final ProductService productService;

    @Operation(summary = "전체 상품 목록 조회")
    @GetMapping(params = "!itemName")
    public List<ProductResponse> getProducts() {
        return productService.findAllProducts();
    }

    @Operation(summary = "상품명 단건 검색")
    @GetMapping(params = "itemName")
    public ProductResponse getProduct(@RequestParam("itemName") @NotBlank(message = "상품명을 입력해주세요.") String itemName) {
        return productService.findProductByItemName(itemName);
    }

    @Operation(summary = "장바구니 상품 일괄 구매")
    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse purchase(@RequestBody @NotEmpty(message = "구매할 상품을 입력해주세요.")
            List<@NotNull(message = "구매할 상품을 입력해주세요.") @Valid PurchaseRequest> items) {
        return productService.purchaseProducts(items);
    }
}
