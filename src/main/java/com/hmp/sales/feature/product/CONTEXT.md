# Product Catalog & Manufacturing Variants — Implementation Context
> Written: 2026-11-15 | Author: @antigravity

## Business Context
The Product module manages roofing sheet manufacturing products (`Tôn sóng vuông, tôn lợp mái, phụ kiện`) along with hierarchical categories, manufacturing specifications, master data (brands, units, colors), and product imagery. It allows Managers to configure product variants, color swatches, and unit prices, while controlling public price visibility dynamically based on user permissions.

## Technical Decisions
- **File-system Category Hierarchy**: Implemented folder-like category navigation where Managers can create either child categories or products under any category node.
- **Permission-driven Price Masking (`PRODUCT_PRICE_VIEW`)**: Evaluated dynamically via `PermissionEvaluationService`; price fields are stripped (null) and sorting by price (`sortBy=price`) is blocked when callers lack permission to prevent indirect price leakage.
- **Decoupled Pricing & Variant Model**: Manufacturing dimensions (such as sheet length) belong to customer orders (`OrderItem`), while static variants (`ProductVariant`) define thickness, wave type, and sheet width (`width`), bound to multi-unit pricing (`ProductPrice`) and color swatches (`ProductColor`).
- **Direct Cloudflare R2 Uploads for Imagery**: Integrated S3-compatible Cloudflare R2 with automatic UUID naming, folder sanitization, MIME verification, and 1-step multipart endpoints for single/batch images and product creation with images.
- **Search Invalidation Event Wiring**: Any status change, product creation, or primary image alteration fires `SearchIndexInvalidationEvent` to keep the in-memory Vietnamese search cache synchronized in real-time.
- **Soft-delete Cascading with Incomplete Order Protection**: Soft-deleting a product cascades to variants and images, but is rejected with HTTP 409 Conflict if active incomplete orders (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`) reference the product.
- **Dedicated Featured Products Module (`featured_products`)**: Decoupled showcase/featured product selection from the primary `products` table into a dedicated entity `FeaturedProduct`. Enables Managers to curate homepage showcase items, define custom ordering (`displayOrder`), and toggle display status (`ACTIVE`/`INACTIVE`) without modifying original product schemas. Soft-deleting a product automatically cascades to its featured product entry.

## Considered and Rejected
- **Storing images as Base64 in PostgreSQL**: Rejected because large binary payloads degrade database performance, increase backup sizes, and prevent CDN caching.
- **Pre-signed URL direct frontend upload**: Rejected for V1 to ensure backend validation of business limits (maximum 1 primary and 10 secondary images per product) before persisting objects.
- **Hard-coding Customer price hiding**: Rejected because price visibility is a business policy configurable by Admin via `PRODUCT_PRICE_VIEW` permission rather than a static role constraint.
- **Allowing sorting by price without permission**: Rejected because observing product sort order leaks relative price rankings even when price amounts are masked.

## Dependencies
- Depends on:
  - `feature/category` (parent category relationships)
  - `feature/file` (`FileStorageService` for Cloudflare R2 image persistence)
  - `feature/search` (`SearchIndexInvalidationEvent` for RAM search cache sync)
  - `feature/systemlog` (`SystemLogService` for audit trails)
  - `feature/user` (`UserRepository` for creator auditing)
  - `security` (`PermissionEvaluationService` for dynamic `PRODUCT_PRICE_VIEW`)
- Depended by:
  - `feature/order` (validates active products, variants, colors, and unit prices when creating orders)
  - `feature/category` (product counts and cascade lock/delete validation)

## Known Limitations
- ⚠️ Cloudflare R2 deletion: When an image is soft-deleted from the database, the physical object on R2 is retained for archival/audit purposes rather than immediately purged from object storage.
- ⚠️ Image dimensions/crop processing: Backend verifies file size (<= 5MB) and MIME type, but does not perform server-side image resizing or cropping.

## Refactor Log

### 2026-11-16 | @antigravity
- Implemented Dedicated Featured Products Module (`featured_products`):
  - Created entity `FeaturedProduct` (`featured_id`, `product_id`, `display_order`, `status`, `created_by`, `created_at`, `updated_at`, `is_deleted`).
  - Added enum `FeaturedProductStatus` (`ACTIVE`, `INACTIVE`) and repository `FeaturedProductRepository`.
  - Added DTO records `FeaturedProductCreateRequest`, `FeaturedProductUpdateRequest`, and `FeaturedProductResponse`.
  - Implemented service layer `FeaturedProductService` & `FeaturedProductServiceImpl` enforcing:
    - Only `ACTIVE` and non-deleted products can be featured.
    - Strict product uniqueness among active featured entries (`existsByProductIdAndIsDeletedFalse`).
    - Dynamic price visibility enforcement via `PermissionEvaluationService` (`PRODUCT_PRICE_VIEW`).
    - Audit logging via `SystemLogService` (`FEATURED_PRODUCT_ADDED`, `FEATURED_PRODUCT_UPDATED`, `FEATURED_PRODUCT_DELETED`).
    - Automatic cascade soft-delete in `ProductServiceImpl.deleteProduct` cleaning up orphaned featured records.
  - Implemented REST controllers:
    - `PublicFeaturedProductController`: `GET /api/v1/featured-products` for homepage/public client access.
    - `ManagerFeaturedProductController`: full CRUD (`GET`, `POST`, `PUT`, `DELETE`) under `/api/v1/manager/featured-products`.
  - Integrated permissions into `DataInitializer` (`FEATURED_PRODUCT_VIEW_ALL`, `FEATURED_PRODUCT_MANAGE`, `FEATURED_PRODUCT_UPDATE`, `FEATURED_PRODUCT_DELETE`) mapped to Admin and Manager roles.
  - Whitelisted `GET /api/v1/featured-products/**` in `SecurityConfig`.
  - Comprehensive unit and controller test suites created (19/19 tests passing).

- Added `width` attribute (khổ rộng tôn, `DECIMAL(10,3)`) to `ProductVariant` entity and related DTOs (`ProductVariantRequest`, `ProductVariantResponse`):
  - Allows Managers to persist standard sheet widths (e.g. 1070mm) for manufacturing roofing variants.
  - Updated `ProductServiceImpl` to set and update `width` during variant creation and editing.
  - Maintained backward compatibility via overloaded DTO constructors.
- Enforced strict file count validation on multipart requests across all Product image endpoints:
  - `ProductImageUploadMultipartRequest`: converted scalar `MultipartFile` bindings to `List<MultipartFile>` to prevent Spring DataBinder from silently discarding excess files when clients select multiple files in Postman or send duplicate keys (`image`, `file`, `images`, `files`).
  - Added double-layer validation in both `ManagerProductController` and `ProductServiceImpl` enforcing at most 1 file for single upload, at most 1 primary image in product creation, and at most 10 secondary images in product creation and batch upload.
  - Rejection with HTTP 400 `BusinessRuleException` on exceeding file limits.
- Handled `MaxUploadSizeExceededException` globally in `GlobalExceptionHandler` returning HTTP 400 Bad Request with a clear Vietnamese error message when uploaded files exceed the 5MB single file or 55MB request limit.

### 2026-11-15 | @antigravity
- Added direct Cloudflare R2 image upload capabilities (`MultipartFile`) for single and batch product images, alongside 1-step product creation with primary and secondary images.
- Standardized Manager Product multipart endpoints (`createProductWithFiles`, `uploadProductImageWithFile`, `uploadProductImagesBatchWithFiles`) using dedicated DTO records (`ProductCreateUploadRequest`, `ProductImageUploadMultipartRequest`, `ProductImageBatchUploadRequest`) bound via `@ModelAttribute`, eliminating loose parameter lists.
- Standardized Admin Color management multipart endpoints (`createColorWithFile`, `updateColorWithFile`) using a unified DTO record (`ColorUploadRequest`) bound via `@ModelAttribute`, enforcing a strict business rule of at most 1 swatch image per color.
- Maintained 100% backward compatibility for JSON payloads accepting pre-existing image URLs.
- Created `CONTEXT.md` documentation for the `product` feature.

