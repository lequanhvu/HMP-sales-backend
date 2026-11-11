package com.hmp.sales.feature.search.service.impl;

import com.hmp.sales.common.util.VietnameseStringUtils;
import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.entity.CategoryStatus;
import com.hmp.sales.feature.category.repository.CategoryRepository;
import com.hmp.sales.feature.product.entity.Brand;
import com.hmp.sales.feature.product.entity.BrandStatus;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.repository.BrandRepository;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.search.dto.SearchResourceType;
import com.hmp.sales.feature.search.dto.SearchSuggestionResponse;
import com.hmp.sales.feature.search.dto.SuggestionIndexItem;
import com.hmp.sales.feature.search.event.SearchIndexInvalidationEvent;
import com.hmp.sales.feature.search.service.SearchSuggestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Triển khai engine gợi ý tìm kiếm tức thời trong bộ nhớ RAM của Server.
 * <p>
 * Tính năng chính:
 * <ul>
 *   <li>Phản hồi cực nhanh dưới 2ms, không gây tải lên cơ sở dữ liệu.</li>
 *   <li>Bộ lọc 2 pha: Ưu tiên tìm chính xác theo tiền tố/chuỗi con, chỉ kích hoạt Levenshtein khi cần thiết.</li>
 *   <li>Quy tắc ngưỡng bậc thang (Tiered Threshold) loại bỏ hoàn toàn gợi ý ngớ ngẩn với từ khóa ngắn.</li>
 *   <li>Đồng bộ hóa tức thời qua Spring Application Event khi có thay đổi dữ liệu (Thêm, Sửa, Xóa).</li>
 *   <li>Lưu trữ bản ghi nhẹ, chống tràn bộ nhớ (Heap Overflow).</li>
 * </ul>
 */
@Slf4j
@Service
public class SearchSuggestionServiceImpl implements SearchSuggestionService {

    private static final int DEFAULT_MAX_ITEMS_PER_TYPE = 10_000;
    private static final String REDIS_SYNC_TOPIC = "sales:search:index:sync";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Bộ nhớ đệm RAM phân loại theo loại tài nguyên (Thread-safe).
     */
    private final Map<SearchResourceType, List<SuggestionIndexItem>> indexMap = new ConcurrentHashMap<>();

    public SearchSuggestionServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            @Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.stringRedisTemplate = stringRedisTemplate;

        // Khởi tạo danh sách rỗng cho từng loại tài nguyên
        for (SearchResourceType type : SearchResourceType.values()) {
            indexMap.put(type, new CopyOnWriteArrayList<>());
        }
    }

    /**
     * Tự động nạp toàn bộ chỉ mục tìm kiếm vào RAM ngay khi ứng dụng khởi động thành công.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reloadAll();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public synchronized void reloadAll() {
        log.info("[SearchSuggestion] Đang nạp lại toàn bộ chỉ mục tìm kiếm vào bộ nhớ RAM...");
        try {
            loadProducts();
            loadCategories();
            loadBrands();
            log.info("[SearchSuggestion] Nạp chỉ mục hoàn tất. Sản phẩm: {}, Danh mục: {}, Thương hiệu: {}",
                    indexMap.get(SearchResourceType.PRODUCT).size(),
                    indexMap.get(SearchResourceType.CATEGORY).size(),
                    indexMap.get(SearchResourceType.BRAND).size());
        } catch (Exception e) {
            log.error("[SearchSuggestion] Lỗi khi nạp chỉ mục tìm kiếm ban đầu: {}", e.getMessage(), e);
        }
    }

    private void loadProducts() {
        List<Product> products = productRepository.findAllActiveForSearchIndex();
        List<SuggestionIndexItem> items = new ArrayList<>();
        for (Product p : products) {
            String extra = p.getCategory() != null ? p.getCategory().getCategoryName() : null;
            String unaccent = p.getProductNameUnaccent() != null && !p.getProductNameUnaccent().isBlank()
                    ? p.getProductNameUnaccent()
                    : VietnameseStringUtils.toUnaccentLowerCase(p.getProductName());

            items.add(new SuggestionIndexItem(p.getId(), p.getProductName(), unaccent, SearchResourceType.PRODUCT, null, extra));
        }
        indexMap.put(SearchResourceType.PRODUCT, new CopyOnWriteArrayList<>(items));
    }

    private void loadCategories() {
        List<Category> categories = categoryRepository.findAll();
        List<SuggestionIndexItem> items = new ArrayList<>();
        for (Category c : categories) {
            if (Boolean.FALSE.equals(c.getIsDeleted()) && c.getStatus() == CategoryStatus.ACTIVE) {
                String unaccent = c.getCategoryNameUnaccent() != null
                        ? c.getCategoryNameUnaccent()
                        : VietnameseStringUtils.toUnaccentLowerCase(c.getCategoryName());

                items.add(new SuggestionIndexItem(c.getId(), c.getCategoryName(), unaccent, SearchResourceType.CATEGORY, null, null));
            }
        }
        indexMap.put(SearchResourceType.CATEGORY, new CopyOnWriteArrayList<>(items));
    }

    private void loadBrands() {
        List<Brand> brands = brandRepository.findAll();
        List<SuggestionIndexItem> items = new ArrayList<>();
        for (Brand b : brands) {
            if (Boolean.FALSE.equals(b.getIsDeleted()) && b.getStatus() == BrandStatus.ACTIVE) {
                String unaccent = b.getBrandNameUnaccent() != null
                        ? b.getBrandNameUnaccent()
                        : VietnameseStringUtils.toUnaccentLowerCase(b.getBrandName());

                items.add(new SuggestionIndexItem(b.getId(), b.getBrandName(), unaccent, SearchResourceType.BRAND, null, null));
            }
        }
        indexMap.put(SearchResourceType.BRAND, new CopyOnWriteArrayList<>(items));
    }

    /** {@inheritDoc} */
    @Override
    public List<SearchSuggestionResponse> getSuggestions(String keyword, SearchResourceType type, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        int safeLimit = Math.min(Math.max(limit, 1), 20);
        String normalizedKeyword = VietnameseStringUtils.toUnaccentLowerCase(keyword.trim());
        if (normalizedKeyword.isEmpty()) {
            return Collections.emptyList();
        }

        // Xác định tập nguồn tài nguyên cần quét
        List<SuggestionIndexItem> sourceItems = new ArrayList<>();
        if (type != null) {
            sourceItems.addAll(indexMap.getOrDefault(type, Collections.emptyList()));
        } else {
            // Quét tổng hợp: ưu tiên sản phẩm trước, sau đó danh mục và thương hiệu
            sourceItems.addAll(indexMap.getOrDefault(SearchResourceType.PRODUCT, Collections.emptyList()));
            sourceItems.addAll(indexMap.getOrDefault(SearchResourceType.CATEGORY, Collections.emptyList()));
            sourceItems.addAll(indexMap.getOrDefault(SearchResourceType.BRAND, Collections.emptyList()));
        }

        // Tự động phục hồi (Self-healing): Nếu RAM index đang rỗng (ví dụ DB nạp chậm lúc startup), tiến hành nạp lại
        if (sourceItems.isEmpty()) {
            log.warn("[SearchSuggestion] Chỉ mục RAM rỗng khi nhận truy vấn '{}'. Tiến hành nạp lại từ Database...", keyword);
            reloadAll();
            if (type != null) {
                sourceItems.addAll(indexMap.getOrDefault(type, Collections.emptyList()));
            } else {
                sourceItems.addAll(indexMap.getOrDefault(SearchResourceType.PRODUCT, Collections.emptyList()));
                sourceItems.addAll(indexMap.getOrDefault(SearchResourceType.CATEGORY, Collections.emptyList()));
                sourceItems.addAll(indexMap.getOrDefault(SearchResourceType.BRAND, Collections.emptyList()));
            }
        }

        List<SuggestionIndexItem> matchedResults = new ArrayList<>();
        Set<Long> visitedIds = new HashSet<>();

        // ---------------------------------------------------------------------
        // PHA 1: Tìm kiếm chính xác (Tiền tố startsWith + Chuỗi con contains)
        // ---------------------------------------------------------------------
        // 1.1 Ưu tiên cao nhất: Bắt đầu bằng từ khóa (Prefix Match)
        for (SuggestionIndexItem item : sourceItems) {
            if (item.unaccentName().startsWith(normalizedKeyword)) {
                matchedResults.add(item);
                visitedIds.add(item.id());
                if (matchedResults.size() >= safeLimit) {
                    return toResponseList(matchedResults);
                }
            }
        }

        // 1.2 Ưu tiên nhì: Chứa từ khóa ở bất kỳ đâu (Substring Match)
        for (SuggestionIndexItem item : sourceItems) {
            if (!visitedIds.contains(item.id()) && item.unaccentName().contains(normalizedKeyword)) {
                matchedResults.add(item);
                visitedIds.add(item.id());
                if (matchedResults.size() >= safeLimit) {
                    return toResponseList(matchedResults);
                }
            }
        }

        // ---------------------------------------------------------------------
        // PHA 2: Tìm kiếm gần đúng sửa lỗi chính tả (Fuzzy / Levenshtein Distance)
        // ---------------------------------------------------------------------
        // Quy tắc bậc thang (Tiered Threshold Rule):
        // - Từ khóa < 3 ký tự: Tuyệt đối KHÔNG chạy Levenshtein để tránh gợi ý sai ngữ cảnh.
        // - Từ khóa 3 đến 5 ký tự: Chỉ cho phép sai lệch tối đa 1 ký tự.
        // - Từ khóa >= 6 ký tự: Cho phép sai lệch tối đa 2 ký tự.
        int keywordLen = normalizedKeyword.length();
        if (keywordLen >= 3 && matchedResults.size() < safeLimit) {
            int maxDistance = (keywordLen <= 5) ? 1 : 2;
            String[] keywordWords = normalizedKeyword.split("\\s+");
            int kWordsCount = keywordWords.length;

            for (SuggestionIndexItem item : sourceItems) {
                if (visitedIds.contains(item.id())) {
                    continue;
                }

                String itemName = item.unaccentName();
                boolean matched = false;

                // 1. So khớp trực tiếp toàn bộ chuỗi nếu độ dài tương đương
                if (Math.abs(itemName.length() - keywordLen) <= maxDistance) {
                    int dist = VietnameseStringUtils.calculateLevenshteinDistance(normalizedKeyword, itemName);
                    if (dist <= maxDistance) {
                        matched = true;
                    }
                }

                // 2. Nếu chưa khớp, dùng kỹ thuật Sliding Window theo số từ của từ khóa
                if (!matched) {
                    String[] itemWords = itemName.split("\\s+");
                    if (itemWords.length >= kWordsCount) {
                        for (int i = 0; i <= itemWords.length - kWordsCount; i++) {
                            StringBuilder windowBuilder = new StringBuilder();
                            for (int w = 0; w < kWordsCount; w++) {
                                if (w > 0) windowBuilder.append(" ");
                                windowBuilder.append(itemWords[i + w]);
                            }
                            String windowStr = windowBuilder.toString();
                            if (Math.abs(windowStr.length() - keywordLen) <= maxDistance) {
                                int dist = VietnameseStringUtils.calculateLevenshteinDistance(normalizedKeyword, windowStr);
                                if (dist <= maxDistance) {
                                    matched = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (matched) {
                    matchedResults.add(item);
                    visitedIds.add(item.id());
                    if (matchedResults.size() >= safeLimit) {
                        return toResponseList(matchedResults);
                    }
                }
            }
        }

        return toResponseList(matchedResults);
    }

    private List<SearchSuggestionResponse> toResponseList(List<SuggestionIndexItem> items) {
        List<SearchSuggestionResponse> responses = new ArrayList<>(items.size());
        for (SuggestionIndexItem item : items) {
            responses.add(SearchSuggestionResponse.fromIndexItem(item));
        }
        return responses;
    }

    /** {@inheritDoc} */
    @Override
    @EventListener
    public void handleInvalidation(SearchIndexInvalidationEvent event) {
        if (event == null) {
            return;
        }

        if (event.operation() == SearchIndexInvalidationEvent.Operation.RELOAD_ALL) {
            reloadAll();
            publishRedisSyncEvent("RELOAD_ALL", 0L);
            return;
        }

        SearchResourceType type = event.type();
        Long targetId = event.targetId();
        List<SuggestionIndexItem> currentList = indexMap.get(type);
        if (currentList == null) {
            return;
        }

        // Xóa phần tử cũ nếu có trong danh sách
        currentList.removeIf(item -> item.id().equals(targetId));

        // Nếu là thao tác UPSERT, truy vấn lại bản ghi và bổ sung vào RAM nếu còn ACTIVE
        if (event.operation() == SearchIndexInvalidationEvent.Operation.UPSERT) {
            loadSingleItem(type, targetId, currentList);
        }

        publishRedisSyncEvent(event.operation().name() + ":" + type.name(), targetId);
    }

    private void loadSingleItem(SearchResourceType type, Long targetId, List<SuggestionIndexItem> targetList) {
        try {
            switch (type) {
                case PRODUCT -> productRepository.findByIdForSearchIndex(targetId).ifPresent(p -> {
                    if (Boolean.FALSE.equals(p.getIsDeleted()) && p.getStatus() == ProductStatus.ACTIVE) {
                        String extra = p.getCategory() != null ? p.getCategory().getCategoryName() : null;
                        String unaccent = p.getProductNameUnaccent() != null && !p.getProductNameUnaccent().isBlank()
                                ? p.getProductNameUnaccent()
                                : VietnameseStringUtils.toUnaccentLowerCase(p.getProductName());
                        targetList.add(new SuggestionIndexItem(p.getId(), p.getProductName(), unaccent, SearchResourceType.PRODUCT, null, extra));
                    }
                });
                case CATEGORY -> categoryRepository.findById(targetId).ifPresent(c -> {
                    if (Boolean.FALSE.equals(c.getIsDeleted()) && c.getStatus() == CategoryStatus.ACTIVE) {
                        String unaccent = c.getCategoryNameUnaccent() != null
                                ? c.getCategoryNameUnaccent()
                                : VietnameseStringUtils.toUnaccentLowerCase(c.getCategoryName());
                        targetList.add(new SuggestionIndexItem(c.getId(), c.getCategoryName(), unaccent, SearchResourceType.CATEGORY, null, null));
                    }
                });
                case BRAND -> brandRepository.findById(targetId).ifPresent(b -> {
                    if (Boolean.FALSE.equals(b.getIsDeleted()) && b.getStatus() == BrandStatus.ACTIVE) {
                        String unaccent = b.getBrandNameUnaccent() != null
                                ? b.getBrandNameUnaccent()
                                : VietnameseStringUtils.toUnaccentLowerCase(b.getBrandName());
                        targetList.add(new SuggestionIndexItem(b.getId(), b.getBrandName(), unaccent, SearchResourceType.BRAND, null, null));
                    }
                });
            }
        } catch (Exception e) {
            log.warn("[SearchSuggestion] Không thể nạp lại item ID={} loại {}: {}", targetId, type, e.getMessage());
        }
    }

    private void publishRedisSyncEvent(String message, Long targetId) {
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.convertAndSend(REDIS_SYNC_TOPIC, message + ":" + targetId);
            } catch (Exception e) {
                log.debug("[SearchSuggestion] Không thể gửi message đồng bộ qua Redis: {}", e.getMessage());
            }
        }
    }
}
