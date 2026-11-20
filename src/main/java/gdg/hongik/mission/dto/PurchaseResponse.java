package gdg.hongik.mission.dto;

import java.util.List;

public record PurchaseResponse(
        long totalPrice,
        List<PurchaseItemResponse> items
) {

    public record PurchaseItemResponse(
            String itemName,
            int count,
            long price
    ) {
    }
}