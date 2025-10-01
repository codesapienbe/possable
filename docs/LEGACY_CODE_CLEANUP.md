# Legacy Code Cleanup Guide

## Overview
This document provides a step-by-step guide for removing legacy code after the Spring Modulith migration is complete and tested.

---

## Phase 1: Identify Legacy Code ✅

### Entities (Old Location: `com.possable.model`)
**Status**: ❌ TO BE DELETED (Superseded by module-internal entities)

- [x] `OrderEntity.java` → Moved to `com.possable.order.internal.OrderEntity`
- [x] `OrderItemEntity.java` → Moved to `com.possable.order.internal.OrderItemEntity`
- [x] `ItemEntity.java` → Moved to `com.possable.inventory.internal.ItemEntity`
- [x] `PrintJobEntity.java` → Moved to `com.possable.print.internal.PrintJobEntity`
- [x] `PrinterEntity.java` → Moved to `com.possable.print.internal.PrinterEntity`
- [x] `PrintTemplateEntity.java` → Moved to `com.possable.print.internal.PrintTemplateEntity`
- [ ] `UserProfileEntity.java` → Will move to user module
- [ ] `UserCredentialEntity.java` → Will move to user module
- [ ] `UserRoleEntity.java` → Will move to user module
- [ ] `UserRoleId.java` → Will move to user module
- [ ] `ApiKeyEntity.java` → Will move to user module

### Repositories (Old Location: `com.possable.repository`)
**Status**: ❌ TO BE DELETED (Superseded by module-internal repositories)

- [x] `OrderRepository.java` → Moved to `com.possable.order.internal.OrderRepository`
- [x] `OrderItemRepository.java` → Moved to `com.possable.order.internal.OrderItemRepository`
- [x] `ItemRepository.java` → Moved to `com.possable.inventory.internal.ItemRepository`
- [x] `PrintJobRepository.java` → Moved to `com.possable.print.internal.PrintJobRepository`
- [x] `PrinterRepository.java` → Moved to `com.possable.print.internal.PrinterRepository`
- [x] `PrintTemplateRepository.java` → Moved to `com.possable.print.internal.PrintTemplateRepository`
- [ ] `UserProfileRepository.java` → Will move to user module
- [ ] `UserCredentialRepository.java` → Will move to user module
- [ ] `UserRoleRepository.java` → Will move to user module
- [ ] `ApiKeyRepository.java` → Will move to user module

### Services (Old Location: `com.possable.service`)
**Status**: ❌ TO BE DELETED (Superseded by module services)

- [x] `OrderService.java` → Replaced by `OrderModuleService` + `OrderFacade`
- [x] `ItemService.java` → Replaced by `InventoryModuleService` + `InventoryFacade`
- [x] `PaymentService.java` → Replaced by `CheckoutModuleService` + `CheckoutFacade`
- [x] `EmployeeService.java` → Replaced by `EmployeeModuleService` + `EmployeeFacade`
- [ ] `PrinterService.java` → TO BE INTEGRATED into `PrintModuleService`
- [ ] `PrintTemplateService.java` → TO BE INTEGRATED into `PrintModuleService`
- [ ] `PrintJobService.java` → TO BE REFACTORED (contains SSE logic - keep SSE, move print job logic)
- [ ] `UserService.java` → Move to user module
- [ ] `UsageService.java` → Move to usage/monitoring module
- ✅ `Broadcaster.java` → **KEEP** (shared infrastructure)
- ✅ `DemoNotificationService.java` → **KEEP** (demo/test utility)

---

## Phase 2: Update Vaadin Views ⏳

### Views to Update (Location: `com.possable.view`)
All views need to be updated to use module facades instead of legacy services.

#### Priority 1: Core Views
- [ ] `OrderView.java` → Use `OrderFacade` instead of `OrderService`
- [ ] `OrdersComponent.java` → Use `OrderFacade`
- [ ] `OrdersDetailComponent.java` → Use `OrderFacade`
- [ ] `ItemListComponent.java` → Use `InventoryFacade` instead of `ItemService`
- [ ] `ItemListLayout.java` → Use `InventoryFacade`
- [ ] `PrintJobsComponent.java` → Use `PrintFacade` instead of `PrintJobService`
- [ ] `PrintJobsView.java` → Use `PrintFacade`
- [ ] `PrintersComponent.java` → Use `PrintFacade` instead of `PrinterService`

#### Priority 2: Dashboard Views
- [ ] `DashboardView.java` → Update service dependencies
- [ ] `CashierView.java` → Use `CheckoutFacade`
- [ ] `KitchenView.java` → Use `OrderFacade` + `PrintFacade`
- [ ] `ServiceView.java` → Update service dependencies

#### Priority 3: Management Views
- [ ] `ManagementView.java` → Update service dependencies
- [ ] `UserAdminView.java` → Use future `UserFacade`
- [ ] `ProfileView.java` → Use future `UserFacade`

#### Infrastructure Views
- ✅ `MainLayout.java` → Minimal changes needed
- ✅ `EntryPointView.java` → Authentication view
- ✅ `AccessDeniedView.java` → Static view
- ✅ `CustomerView.java` → Minimal business logic
- ✅ `RoleDashboardFactory.java` → Routing logic
- ✅ `PatternLockComponent.java` → UI component
- ✅ `LogViewerComponent.java` → Infrastructure component

---

## Phase 3: Integration into Modules ⏳

### Print Module Integration
**Task**: Integrate PrinterService and PrintTemplateService into PrintModuleService

```java
// PrintModuleService needs to add:
public record Printer(String id, String name, String category, String description, Instant createdAt) {}
public record Template(String id, String printerCategory, String templateName, String content, Instant createdAt) {}

// Methods to add:
public Printer registerPrinter(String name, String category, String description)
public List<Printer> listPrinters(Map<String, String> filters)
public Printer findPrinterById(String id)
public Template createTemplate(String printerCategory, String templateName, String content)
public List<Template> listTemplates(Map<String, String> filters)
public Template findTemplateById(String id)
```

**Status**: ⏳ In Progress

### PrintJobService Refactoring
**Task**: Extract SSE functionality to separate infrastructure service

Current `PrintJobService` contains:
1. Print job business logic → Move to `PrintModuleService`
2. SSE event streaming → Keep as separate `PrintEventStreamService`
3. Metrics collection → Keep for monitoring

**Status**: ⏳ Pending

---

## Phase 4: Create User Module ⏳

### New Module: `com.possable.user`

**Tables OWNED**:
- `user_profiles`
- `user_credentials`
- `user_roles`
- `api_keys`

**Public API**: `UserFacade`

**Components to Create**:
- `src/main/java/com/possable/user/internal/UserProfileEntity.java`
- `src/main/java/com/possable/user/internal/UserCredentialEntity.java`
- `src/main/java/com/possable/user/internal/UserRoleEntity.java`
- `src/main/java/com/possable/user/internal/ApiKeyEntity.java`
- `src/main/java/com/possable/user/internal/UserProfileRepository.java`
- `src/main/java/com/possable/user/internal/UserCredentialRepository.java`
- `src/main/java/com/possable/user/internal/UserRoleRepository.java`
- `src/main/java/com/possable/user/internal/ApiKeyRepository.java`
- `src/main/java/com/possable/user/internal/UserModuleService.java`
- `src/main/java/com/possable/user/UserFacade.java`
- `src/main/java/com/possable/user/package-info.java`

**Status**: ⏳ Pending

---

## Phase 5: Delete Legacy Code ❌

### Deletion Checklist

#### Step 1: Verify All Tests Pass
```bash
mvn clean test
```

#### Step 2: Verify No Compilation Errors
```bash
mvn clean compile
```

#### Step 3: Delete Old Model Package
```bash
rm -rf src/main/java/com/possable/model/OrderEntity.java
rm -rf src/main/java/com/possable/model/OrderItemEntity.java
rm -rf src/main/java/com/possable/model/ItemEntity.java
rm -rf src/main/java/com/possable/model/PrintJobEntity.java
rm -rf src/main/java/com/possable/model/PrinterEntity.java
rm -rf src/main/java/com/possable/model/PrintTemplateEntity.java
```

#### Step 4: Delete Old Repository Package
```bash
rm -rf src/main/java/com/possable/repository/OrderRepository.java
rm -rf src/main/java/com/possable/repository/OrderItemRepository.java
rm -rf src/main/java/com/possable/repository/ItemRepository.java
rm -rf src/main/java/com/possable/repository/PrintJobRepository.java
rm -rf src/main/java/com/possable/repository/PrinterRepository.java
rm -rf src/main/java/com/possable/repository/PrintTemplateRepository.java
```

#### Step 5: Delete Old Service Classes
```bash
rm -rf src/main/java/com/possable/service/OrderService.java
rm -rf src/main/java/com/possable/service/ItemService.java
rm -rf src/main/java/com/possable/service/PaymentService.java
rm -rf src/main/java/com/possable/service/EmployeeService.java
rm -rf src/main/java/com/possable/service/PrinterService.java
rm -rf src/main/java/com/possable/service/PrintTemplateService.java
```

#### Step 6: Clean Up Test Files
Update all test files to use new module facades instead of old services.

---

## Phase 6: Final Verification ✅

### Verification Steps

1. **Compile Application**
   ```bash
   mvn clean compile
   ```

2. **Run Tests**
   ```bash
   mvn test
   ```

3. **Run Module Structure Test**
   ```bash
   mvn test -Dtest=ModuleStructureTest
   ```

4. **Start Application**
   ```bash
   mvn spring-boot:run
   ```

5. **Verify Module Boundaries**
   Check logs for Spring Modulith verification:
   ```
   INFO: Spring Modulith - Module structure verified successfully
   ```

6. **Integration Tests**
   Test all major flows:
   - Order creation → Print job creation
   - Order completion → Payment processing
   - Payment completion → Customer notification

---

## Rollback Plan

If issues are discovered:

1. **Don't Delete Files Immediately**
   - Mark files as `@Deprecated` first
   - Run application for 1-2 sprints
   - Monitor for issues

2. **Keep Git History**
   ```bash
   git log --follow <file-path>
   ```

3. **Restore if Needed**
   ```bash
   git checkout <commit-hash> -- <file-path>
   ```

---

## Timeline

| Phase | Status | ETA |
|-------|--------|-----|
| 1. Identify Legacy Code | ✅ Complete | Done |
| 2. Update Vaadin Views | ⏳ In Progress | 2-3 days |
| 3. Integration into Modules | ⏳ In Progress | 1-2 days |
| 4. Create User Module | ⏳ Pending | 1 day |
| 5. Delete Legacy Code | ❌ Pending | 1 day |
| 6. Final Verification | ❌ Pending | 1 day |

**Total Estimated Time**: 6-9 days

---

## Post-Cleanup Benefits

✅ **Cleaner Codebase**: Single source of truth for each entity
✅ **Enforced Boundaries**: Spring Modulith prevents violations
✅ **Better Testability**: Each module can be tested in isolation
✅ **Clear Ownership**: Each module owns its data
✅ **Easier Onboarding**: New developers understand module structure
✅ **Microservice Ready**: Modules can be extracted if needed

---

## References

- [Spring Modulith Documentation](https://spring.io/projects/spring-modulith)
- [MODULITH_ARCHITECTURE.md](./MODULITH_ARCHITECTURE.md)
- [MODULITH_MIGRATION_COMPLETE.md](./MODULITH_MIGRATION_COMPLETE.md) 