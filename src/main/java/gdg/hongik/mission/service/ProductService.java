package gdg.hongik.mission.service;

import gdg.hongik.mission.common.BadRequestException;
import gdg.hongik.mission.common.NotFoundException;
import gdg.hongik.mission.dto.ProductRequest;
import gdg.hongik.mission.dto.ProductResponse;
import gdg.hongik.mission.dto.PurchaseRequest;
import gdg.hongik.mission.dto.PurchaseResponse;
import gdg.hongik.mission.entity.Product;
import gdg.hongik.mission.entity.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductResponse> findAllProducts() {
        return repository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findProductByItemName(String itemName) {
        return ProductResponse.from(findOneByItemName(itemName));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String itemName = request.name();

        if (!repository.findByItemName(itemName).isEmpty()) {
            throw new BadRequestException("이미 존재하는 상품명입니다.");
        }

        Product product = new Product();
        product.setItemName(itemName);
        product.setPrice(request.price());
        product.setQuantity(request.stockQuantity());
        product.setCategory(request.category());

        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductResponse addStock(String itemName, Integer quantity) {
        Product product = findOneByItemName(itemName);
        if (quantity > Integer.MAX_VALUE - product.getQuantity()) {
            throw new BadRequestException("추가 후 재고가 허용 범위를 초과합니다.");
        }
        product.setQuantity(product.getQuantity() + quantity);

        return ProductResponse.from(product);
    }

    @Transactional
    public PurchaseResponse purchaseProducts(List<PurchaseRequest> items) {
        Set<Long> selectedIds = new HashSet<>();
        List<PurchaseResponse.PurchaseItemResponse> purchasedItems = new ArrayList<>();
        long totalPrice = 0L;

        for (PurchaseRequest item : items) {
            if (!selectedIds.add(item.id())) {
                throw new BadRequestException("같은 상품은 하나의 항목으로 보내주세요.");
            }

            Product product = repository.findById(item.id())
                    .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다."));

            int count = item.count();

            if (product.getQuantity() < count) {
                throw new BadRequestException("재고가 부족합니다.");
            }

            long itemPrice = (long) product.getPrice() * count;
            if (itemPrice > Long.MAX_VALUE - totalPrice) {
                throw new BadRequestException("총 구매 금액이 허용 범위를 초과합니다.");
            }
            totalPrice += itemPrice;
            product.setQuantity(product.getQuantity() - count);

            purchasedItems.add(new PurchaseResponse.PurchaseItemResponse(
                    product.getItemName(),
                    count,
                    itemPrice
            ));
        }

        return new PurchaseResponse(totalPrice, purchasedItems);
    }

    @Transactional
    public List<ProductResponse> deleteProducts(List<Long> ids) {
        Set<Long> uniqueIds = new HashSet<>(ids);
        List<Product> products = repository.findAllById(uniqueIds);

        if (products.size() != uniqueIds.size()) {
            throw new NotFoundException("삭제할 상품을 찾을 수 없습니다.");
        }

        repository.deleteAll(products);
        return findAllProducts();
    }

    private Product findOneByItemName(String itemName) {
        return repository.findByItemName(itemName).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다."));
    }
}
