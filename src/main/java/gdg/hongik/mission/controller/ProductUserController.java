package gdg.hongik.mission.controller;

import gdg.hongik.mission.dto.Product;
import gdg.hongik.mission.dto.ProductStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
public class ProductUserController {

    // 상품 조회
    @Operation(
            summary = "상품 조회",
            description = "상품명과 정확히 일치하는 상품 하나를 조회합니다. "
                    + "상품 ID, 상품명, 개당 가격, 현재 재고 수량을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "상품 조회 성공")
    @GetMapping("/products")
    @Tag(
            name = "소비자 상품 API",
            description = "소비자의 상품 조회와 구매 기능"
    )

    public ResponseEntity<Product> getProduct(
            @RequestParam("name") String name) {

        for (Product product : ProductStore.products) {
            if (product.getName().equals(name)) {
                return ResponseEntity.ok(product);
            }
        }

        throw new RuntimeException("상품을 찾을 수 없습니다.");
    }

    // 상품 구매
    @Operation(
            summary = "상품 구매",
            description = "items 배열에 productId와 quantity를 담아 "
                    + "하나 이상의 상품을 구매합니다. "
                    + "총 구매 금액과 상품별 이름, 구매 수량, 소비 금액을 반환합니다. "
                    + "상품이 없거나 재고가 부족하면 예외가 발생합니다."
    )
    @ApiResponse(responseCode = "201", description = "상품 구매 성공")
    @PostMapping("/purchases")
    public ResponseEntity<Map<String, Object>> purchase(
            @RequestBody Map<String, List<Map<String, Long>>> request) {

        List<Map<String, Long>> items = request.get("items");

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("구매할 상품을 입력해주세요.");
        }

        List<Product> selectedProducts = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<Map<String, Object>> purchasedItems = new ArrayList<>();

        long totalAmount = 0L;

        // 먼저 모든 상품과 구매 수량을 확인한다.
        for (Map<String, Long> item : items) {

            if (item == null) {
                throw new RuntimeException("구매 항목을 확인해주세요.");
            }

            Long productId = item.get("productId");
            Long quantity = item.get("quantity");

            if (productId == null || quantity == null
                    || quantity <= 0 || quantity > Integer.MAX_VALUE) {
                throw new RuntimeException("상품 ID와 구매 수량을 확인해주세요.");
            }

            Product selectedProduct = null;

            for (Product product : ProductStore.products) {
                if (product.getId().equals(productId)) {
                    selectedProduct = product;
                    break;
                }
            }

            if (selectedProduct == null) {
                throw new RuntimeException("상품을 찾을 수 없습니다.");
            }

            // 같은 상품을 중복해서 요청했는지 확인한다.
            for (Product product : selectedProducts) {
                if (product.getId().equals(productId)) {
                    throw new RuntimeException(
                            "같은 상품은 하나의 항목으로 보내주세요."
                    );
                }
            }

            if (selectedProduct.getStockQuantity() < quantity) {
                throw new RuntimeException("재고가 부족합니다.");
            }

            long amount = (long) selectedProduct.getPrice() * quantity;

            Map<String, Object> purchasedItem = new HashMap<>();
            purchasedItem.put("name", selectedProduct.getName());
            purchasedItem.put("quantity", quantity);
            purchasedItem.put("amount", amount);

            purchasedItems.add(purchasedItem);
            selectedProducts.add(selectedProduct);
            quantities.add(quantity.intValue());

            totalAmount += amount;
        }

        // 모든 상품을 구매할 수 있을 때 재고를 차감한다.
        for (int i = 0; i < selectedProducts.size(); i++) {
            Product product = selectedProducts.get(i);

            product.setStockQuantity(
                    product.getStockQuantity() - quantities.get(i)
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalAmount", totalAmount);
        response.put("items", purchasedItems);

        return ResponseEntity.status(201).body(response);
    }
}