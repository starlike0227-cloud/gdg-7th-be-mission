package gdg.hongik.mission.dto;

import gdg.hongik.mission.entity.Category;
import gdg.hongik.mission.entity.Product;

public record ProductResponse(
        Long id,
        String itemName,
        int price,
        int quantity,
        Category category
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getItemName(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategory()
        );
    }
}