package gdg.hongik.mission.dto;

import java.util.List;

public record DeleteProductsResponse(List<ProductStockResponse> items) {
}