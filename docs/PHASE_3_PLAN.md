# Phase 3: Module View Organization + Legacy Cleanup

**Date**: October 1, 2025  
**Status**: 🚀 **STARTING**

---

## Objective

1. **Move Vaadin views into their respective modules** (SCS pattern)
2. **Delete all legacy code** (old services, repositories, entities)
3. **Ensure only Spring Modulith code remains**

---

## View Organization by Module

### Order Module Views
**Package**: `com.possable.order.ui`

- `OrderView.java` - Main order management interface
- `OrdersComponent.java` - Order listing grid
- `OrdersDetailComponent.java` - Order detail view

**Reason**: These views directly manage orders and should be part of the order module.

---

### Inventory Module Views
**Package**: `com.possable.inventory.ui`

- `ItemListComponent.java` - Menu item tiles
- `ItemListLayout.java` - Item list wrapper

**Reason**: These views manage menu items and belong to inventory module.

---

### Print Module Views
**Package**: `com.possable.print.ui`

- `PrintersComponent.java` - Printer management
- `PrintJobsComponent.java` - Print queue display
- `PrintJobsView.java` - Print jobs page

**Reason**: These views manage printers, templates, and print jobs.

---

### Checkout Module Views
**Package**: `com.possable.checkout.ui`

- `CashierView.java` - Checkout/payment interface

**Reason**: Handles payment and checkout operations.

---

### Employee Module Views
**Package**: `com.possable.employee.ui`

- (Future: Employee management views)

---

### Customer Module Views
**Package**: `com.possable.customer.ui`

- `CustomerView.java` - Customer-facing display

**Reason**: Customer-specific interface.

---

### Shared/Infrastructure Views
**Package**: `com.possable.infrastructure.ui` (new package for cross-module views)

- `DashboardView.java` - Aggregates multiple modules
- `MainLayout.java` - Application layout
- `KitchenView.java` - Aggregates orders + print jobs
- `ServiceView.java` - Service interface
- `ManagementView.java` - Management dashboard
- `EntryPointView.java` - Authentication entry
- `AccessDeniedView.java` - Error view
- `RoleDashboardFactory.java` - Routing logic
- `PatternLockComponent.java` - Shared UI component
- `LogViewerComponent.java` - Infrastructure tool

**Reason**: These views aggregate data from multiple modules or provide infrastructure.

---

### User Module Views (Future)
**Package**: `com.possable.user.ui` (when user module is created)

- `UserAdminView.java` - User management
- `ProfileView.java` - User profile

---

## Legacy Code to Delete

### 1. Legacy Entities (`com.possable.model/`)
**Status**: ❌ DELETE ALL

- [x] `OrderEntity.java` → Moved to `com.possable.order.internal.OrderEntity`
- [x] `OrderItemEntity.java` → Moved to `com.possable.order.internal.OrderItemEntity`
- [x] `ItemEntity.java` → Moved to `com.possable.inventory.internal.ItemEntity`
- [x] `PrintJobEntity.java` → Moved to `com.possable.print.internal.PrintJobEntity`
- [x] `PrinterEntity.java` → Moved to `com.possable.print.internal.PrinterEntity`
- [x] `PrintTemplateEntity.java` → Moved to `com.possable.print.internal.PrintTemplateEntity`
- [ ] `UserProfileEntity.java` → Will move to user module (pending)
- [ ] `UserCredentialEntity.java` → Will move to user module (pending)
- [ ] `UserRoleEntity.java` → Will move to user module (pending)
- [ ] `UserRoleId.java` → Will move to user module (pending)
- [ ] `ApiKeyEntity.java` → Will move to user module (pending)

**Action**: Delete migrated entities, keep user entities for now.

---

### 2. Legacy Repositories (`com.possable.repository/`)
**Status**: ❌ DELETE ALL

- [x] `OrderRepository.java` → Moved to `com.possable.order.internal.OrderRepository`
- [x] `OrderItemRepository.java` → Moved to `com.possable.order.internal.OrderItemRepository`
- [x] `ItemRepository.java` → Moved to `com.possable.inventory.internal.ItemRepository`
- [x] `PrintJobRepository.java` → Moved to `com.possable.print.internal.PrintJobRepository`
- [x] `PrinterRepository.java` → Moved to `com.possable.print.internal.PrinterRepository`
- [x] `PrintTemplateRepository.java` → Moved to `com.possable.print.internal.PrintTemplateRepository`
- [ ] `UserProfileRepository.java` → Keep for now (user module pending)
- [ ] `UserCredentialRepository.java` → Keep for now
- [ ] `UserRoleRepository.java` → Keep for now
- [ ] `ApiKeyRepository.java` → Keep for now

**Action**: Delete migrated repositories, keep user repositories for now.

---

### 3. Legacy Services (`com.possable.service/`)
**Status**: ❌ DELETE SUPERSEDED SERVICES

- [x] `OrderService.java` → **DELETE** (replaced by OrderModuleService)
- [x] `ItemService.java` → **DELETE** (replaced by InventoryModuleService)
- [x] `PaymentService.java` → **DELETE** (replaced by CheckoutModuleService)
- [x] `EmployeeService.java` → **DELETE** (replaced by EmployeeModuleService)
- [x] `PrinterService.java` → **DELETE** (integrated into PrintModuleService)
- [x] `PrintTemplateService.java` → **DELETE** (integrated into PrintModuleService)
- [ ] `PrintJobService.java` → **REFACTOR** (keep SSE functionality, move print logic to module)
- [ ] `UserService.java` → **KEEP** (user module not yet created)
- [ ] `UsageService.java` → **KEEP** (usage module not yet created)
- [x] `Broadcaster.java` → **KEEP** (shared infrastructure)
- [x] `DemoNotificationService.java` → **KEEP** (demo utility)

**Action**: Delete superseded services, keep infrastructure services.

---

### 4. Old View Package (`com.possable.view/`)
**Status**: ⏳ MIGRATE TO MODULES

**Action**: Move views to modules, then delete empty package.

---

## Migration Steps

### Step 1: Create Module UI Packages ✅
```
src/main/java/com/possable/
├── order/ui/
├── inventory/ui/
├── print/ui/
├── checkout/ui/
├── employee/ui/
├── customer/ui/
└── infrastructure/ui/
```

### Step 2: Move Views to Modules ⏳
1. Move order views → `order.ui`
2. Move inventory views → `inventory.ui`
3. Move print views → `print.ui`
4. Move checkout views → `checkout.ui`
5. Move customer views → `customer.ui`
6. Move shared views → `infrastructure.ui`

### Step 3: Update View Dependencies ⏳
- Update all `@Route` annotations
- Update navigation references
- Update MainLayout menu items
- Update RoleDashboardFactory

### Step 4: Delete Legacy Entities ⏳
```bash
rm src/main/java/com/possable/model/OrderEntity.java
rm src/main/java/com/possable/model/OrderItemEntity.java
rm src/main/java/com/possable/model/ItemEntity.java
rm src/main/java/com/possable/model/PrintJobEntity.java
rm src/main/java/com/possable/model/PrinterEntity.java
rm src/main/java/com/possable/model/PrintTemplateEntity.java
```

### Step 5: Delete Legacy Repositories ⏳
```bash
rm src/main/java/com/possable/repository/OrderRepository.java
rm src/main/java/com/possable/repository/OrderItemRepository.java
rm src/main/java/com/possable/repository/ItemRepository.java
rm src/main/java/com/possable/repository/PrintJobRepository.java
rm src/main/java/com/possable/repository/PrinterRepository.java
rm src/main/java/com/possable/repository/PrintTemplateRepository.java
```

### Step 6: Delete Legacy Services ⏳
```bash
rm src/main/java/com/possable/service/OrderService.java
rm src/main/java/com/possable/service/ItemService.java
rm src/main/java/com/possable/service/PaymentService.java
rm src/main/java/com/possable/service/EmployeeService.java
rm src/main/java/com/possable/service/PrinterService.java
rm src/main/java/com/possable/service/PrintTemplateService.java
```

### Step 7: Update Test Files ⏳
- Update all test imports
- Fix broken tests
- Remove tests for deleted services

### Step 8: Final Verification ⏳
```bash
mvn clean compile
mvn test
mvn spring-boot:run
```

---

## File Structure After Migration

```
src/main/java/com/possable/
│
├── Application.java
│
├── config/
│   ├── DemoDataConfig.java
│   ├── SecurityConfig.java
│   ├── ThreadConfig.java
│   └── ...
│
├── controller/                    # REST API Controllers (stay here)
│   ├── OrderController.java
│   ├── ItemController.java
│   ├── PaymentController.java
│   └── ...
│
├── order/                         # Order Module
│   ├── OrderFacade.java          # Public API
│   ├── OrderCreatedEvent.java
│   ├── OrderCompletedEvent.java
│   ├── package-info.java
│   ├── internal/
│   │   ├── OrderModuleService.java
│   │   ├── OrderEntity.java
│   │   ├── OrderItemEntity.java
│   │   └── OrderRepository.java
│   └── ui/                        # NEW: Order Views
│       ├── OrderView.java
│       ├── OrdersComponent.java
│       └── OrdersDetailComponent.java
│
├── inventory/                     # Inventory Module
│   ├── InventoryFacade.java
│   ├── package-info.java
│   ├── internal/
│   │   ├── InventoryModuleService.java
│   │   ├── ItemEntity.java
│   │   └── ItemRepository.java
│   └── ui/                        # NEW: Inventory Views
│       ├── ItemListComponent.java
│       └── ItemListLayout.java
│
├── print/                         # Print Module
│   ├── PrintFacade.java
│   ├── PrintJobRequestedEvent.java
│   ├── package-info.java
│   ├── internal/
│   │   ├── PrintModuleService.java
│   │   ├── PrintJobEntity.java
│   │   ├── PrinterEntity.java
│   │   ├── PrintTemplateEntity.java
│   │   └── *Repository.java
│   └── ui/                        # NEW: Print Views
│       ├── PrintersComponent.java
│       ├── PrintJobsComponent.java
│       └── PrintJobsView.java
│
├── checkout/                      # Checkout Module
│   ├── CheckoutFacade.java
│   ├── PaymentCompletedEvent.java
│   ├── package-info.java
│   ├── internal/
│   │   └── CheckoutModuleService.java
│   └── ui/                        # NEW: Checkout Views
│       └── CashierView.java
│
├── employee/                      # Employee Module
│   ├── EmployeeFacade.java
│   ├── package-info.java
│   ├── internal/
│   │   └── EmployeeModuleService.java
│   └── ui/                        # NEW: (Future employee views)
│
├── customer/                      # Customer Module
│   ├── CustomerFacade.java
│   ├── package-info.java
│   ├── internal/
│   │   └── CustomerModuleService.java
│   └── ui/                        # NEW: Customer Views
│       └── CustomerView.java
│
├── infrastructure/                # NEW: Infrastructure Package
│   └── ui/                        # Shared/Cross-module Views
│       ├── MainLayout.java
│       ├── DashboardView.java
│       ├── KitchenView.java
│       ├── ServiceView.java
│       ├── ManagementView.java
│       ├── EntryPointView.java
│       ├── AccessDeniedView.java
│       ├── RoleDashboardFactory.java
│       ├── PatternLockComponent.java
│       └── LogViewerComponent.java
│
├── service/                       # Shared Services (minimal)
│   ├── Broadcaster.java          # Keep (infrastructure)
│   └── DemoNotificationService.java  # Keep (demo)
│
└── model/                         # ❌ TO BE DELETED (except user entities for now)
    └── repository/                # ❌ TO BE DELETED (except user repos for now)
```

---

## Benefits of This Organization

✅ **True SCS**: Each module contains its own UI  
✅ **Clear Boundaries**: UI follows module boundaries  
✅ **Independent Deployment**: Modules can be extracted with their UIs  
✅ **Team Ownership**: Teams own module + UI  
✅ **Vaadin Routes**: Routes map to modules  
✅ **Code Colocation**: Related code stays together  
✅ **Clean Architecture**: No shared entity/repository packages  

---

## Risks & Mitigation

### Risk 1: Breaking Vaadin Routes
**Mitigation**: Update all `@Route` annotations, test navigation

### Risk 2: Circular Dependencies
**Mitigation**: Use facades for cross-module access, never import internal classes

### Risk 3: Build Failures
**Mitigation**: Incremental changes, compile after each step

---

## Timeline

| Task | Duration | Status |
|------|----------|--------|
| Create module UI packages | 10 min | ⏳ |
| Move order views | 30 min | ⏳ |
| Move inventory views | 20 min | ⏳ |
| Move print views | 20 min | ⏳ |
| Move other views | 30 min | ⏳ |
| Delete legacy entities | 10 min | ⏳ |
| Delete legacy repositories | 10 min | ⏳ |
| Delete legacy services | 10 min | ⏳ |
| Update tests | 1 hour | ⏳ |
| Final verification | 30 min | ⏳ |
| **TOTAL** | **~4 hours** | ⏳ |

---

## Success Criteria

✅ All views in module packages  
✅ No code in `com.possable.model` (except user entities)  
✅ No code in `com.possable.repository` (except user repos)  
✅ Legacy services deleted  
✅ Application compiles  
✅ Tests pass  
✅ Application runs  
✅ All routes work  
✅ Navigation works  

---

## Next Actions

1. Create module `ui` packages
2. Move views incrementally
3. Delete legacy code
4. Update and run tests
5. Create final migration report

Let's proceed! 🚀 