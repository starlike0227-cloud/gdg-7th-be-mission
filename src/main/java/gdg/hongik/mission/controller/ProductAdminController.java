package gdg.hongik.mission.controller;

import gdg.hongik.mission.dto.Product;
import gdg.hongik.mission.dto.ProductStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; //키와 값을 짝지어 저장

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "관리자 상품 API",
        description = "관리자의 상품 등록, 재고 추가, 삭제 기능"
)
@RestController
@RequestMapping("/admin/products")
public class ProductAdminController {

    // 상품 등록
    @Operation(
            summary = "상품 등록",
            description = "상품명(name), 개당 가격(price), "
                    + "초기 재고(stockQuantity)를 입력하여 상품을 등록합니다. "
                    + "동일한 상품명이 존재하면 예외가 발생합니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "상품 등록 성공. 응답 본문은 없습니다."
    )
    @PostMapping
    public ResponseEntity<Void> createProduct(
            @RequestBody Product request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("상품명을 입력해주세요.");
        }

        if (request.getPrice() <= 0 || request.getStockQuantity() < 0) {
            throw new RuntimeException("가격과 초기 재고를 확인해주세요.");
        }

        for (Product product : ProductStore.products) {
            if (product.getName().equals(request.getName())) {
                throw new RuntimeException("이미 존재하는 상품명입니다.");
            }
        }

        Product product = new Product();

        product.setId(ProductStore.sequence++);
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        ProductStore.products.add(product);

        return ResponseEntity.status(201).build();
    }

    // 재고 추가
    @Operation(
            summary = "재고 추가",
            description = "경로의 productId에 해당하는 상품에 "
                    + "요청 본문의 quantityToAdd만큼 재고를 추가합니다. "
                    + "상품명과 추가 후 최종 재고 수량을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "재고 추가 성공")
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<Map<String, Object>> addStock(
            @PathVariable("productId") Long productId,
            @RequestBody Map<String, Integer> request) {

        Integer quantityToAdd = request.get("quantityToAdd");

        if (quantityToAdd == null || quantityToAdd <= 0) {
            throw new RuntimeException("추가 수량은 1 이상이어야 합니다.");
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

        int finalStock = Math.addExact(
                selectedProduct.getStockQuantity(),
                quantityToAdd
        );

        selectedProduct.setStockQuantity(finalStock);

        Map<String, Object> response = new HashMap<>();
        response.put("name", selectedProduct.getName());
        response.put("stockQuantity", selectedProduct.getStockQuantity());

        return ResponseEntity.ok(response);
    }

    // 상품 삭제
    @Operation(
            summary = "상품 삭제",
            description = "요청 본문의 ids 배열에 담긴 상품들을 삭제합니다. "
                    + "삭제 후 남아 있는 상품들의 이름과 재고 수량을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "상품 삭제 성공")
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteProducts(
            @RequestBody Map<String, List<Long>> request) {

        List<Long> ids = request.get("ids");

        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("삭제할 상품 ID를 입력해주세요.");
        }

        // 삭제 전에 모든 상품이 존재하는지 확인한다.
        for (Long id : ids) {
            boolean found = false;

            for (Product product : ProductStore.products) {
                if (product.getId().equals(id)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new RuntimeException("상품을 찾을 수 없습니다.");
            }
        }

        // List를 뒤에서부터 확인하며 삭제한다.
        for (int i = ProductStore.products.size() - 1; i >= 0; i--) {
            Product product = ProductStore.products.get(i);

            if (ids.contains(product.getId())) {
                ProductStore.products.remove(i);
            }
        }

        List<Map<String, Object>> remainingProducts = new ArrayList<>();

        for (Product product : ProductStore.products) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", product.getName());
            item.put("stockQuantity", product.getStockQuantity());

            remainingProducts.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("items", remainingProducts);

        return ResponseEntity.ok(response);
    }
}