# Product Catalog & Manufacturing Variants — Implementation Context
> Written: 2026-11-15 | Author: @antigravity

## Business Context
The Product module manages roofing sheet manufacturing products (`Tôn sóng vuông, tôn lợp mái, phụ kiện`) along with hierarchical categories, manufacturing specifications, master data (brands, units, colors), and product imagery. It allows Managers to configure product variants, color swatches, and unit prices, while controlling public price visibility dynamically based on user permissions.

## Technical Decisions
- **File-system Category Hierarchy**: Implemented folder-like category navigation where Managers can create either child categories or products under any category node.
- **Permission-driven Price Masking (`PRODUCT_PRICE_VIEW`)**: Evaluated dynamically via `PermissionEvaluationService`; price fields are stripped (null) and sorting by price (`sortBy=price`) is blocked when callers lack permission to prevent indirect price leakage.
- **Decoupled Pricing & Variant Model**: Manufacturing dimensions (such as sheet length) belong to customer orders (`OrderItem`), while static variants (`ProductVariant`) define thickness and wave type, bound to multi-unit pricing (`ProductPrice`) and color swatches (`ProductColor`).
- **Direct Cloudflare R2 Uploads for Imagery**: Integrated S3-compatible Cloudflare R2 with automatic UUID naming, folder sanitization, MIME verification, and 1-step multipart endpoints for single/batch images and product creation with images.
- **Search Invalidation Event Wiring**: Any status change, product creation, or primary image alteration fires `SearchIndexInvalidationEvent` to keep the in-memory Vietnamese search cache synchronized in real-time.
- **Soft-delete Cascading with Incomplete Order Protection**: Soft-deleting a product cascades to variants and images, but is rejected with HTTP 409 Conflict if active incomplete orders (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`) reference the product.

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

### 2026-11-15 | @antigravity
- Added direct Cloudflare R2 image upload capabilities (`MultipartFile`) for single and batch product images, alongside 1-step product creation with primary and secondary images.
- Standardized Manager Product multipart endpoints (`createProductWithFiles`, `uploadProductImageWithFile`, `uploadProductImagesBatchWithFiles`) using dedicated DTO records (`ProductCreateUploadRequest`, `ProductImageUploadMultipartRequest`, `ProductImageBatchUploadRequest`) bound via `@ModelAttribute`, eliminating loose parameter lists.
- Standardized Admin Color management multipart endpoints (`createColorWithFile`, `updateColorWithFile`) using a unified DTO record (`ColorUploadRequest`) bound via `@ModelAttribute`, enforcing a strict business rule of at most 1 swatch image per color.
- Maintained 100% backward compatibility for JSON payloads accepting pre-existing image URLs.
- Created `CONTEXT.md` documentation for the `product` feature.

