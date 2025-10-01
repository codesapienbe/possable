# Final Compilation Fix - Remaining Errors

**Remaining Errors**: 18  
**Status**: Ready to fix

---

## Errors to Fix

### 1. **PrintModuleService** - Old Imports (5 errors)
**File**: `src/main/java/com/possable/print/internal/PrintModuleService.java`

**Remove these imports**:
```java
import com.possable.model.PrintJobEntity;  
import com.possable.model.PrinterEntity;  
import com.possable.model.PrintTemplateEntity;  
import com.possable.repository.PrintJobRepository;  
import com.possable.repository.PrinterRepository;  
import com.possable.repository.PrintTemplateRepository;  
import com.possable.service.PrinterService;  
import com.possable.service.PrintTemplateService;  
```

**Use instead**:
```java
// Already in internal package - no imports needed
// PrintJobEntity, PrinterEntity, PrintTemplateEntity are in same package
// Repositories are in same package
```

---

### 2. **OrdersDetailComponent** - Wrong Type (1 error)
**File**: `src/main/java/com/possable/order/ui/OrdersDetailComponent.java`  
**Line 25**

**Change**:
```java
private CheckboxGroup<PrintFacade.PrinterDto> printers;
```

**To**:
```java
private CheckboxGroup<String> printers;  // or remove if unused
```

---

### 3. **OrderView** - PrinterService References (3 errors)
**File**: `src/main/java/com/possable/order/ui/OrderView.java`  
**Lines 187, 195, 204**

**Change**:
```java
CheckboxGroup<PrinterService.Printer> printers = new CheckboxGroup<>();
Set<PrinterService.Printer> selected = printers.getSelectedItems();
for (PrinterService.Printer p : selected) {
```

**To**:
```java
CheckboxGroup<PrintFacade.PrinterDto> printers = new CheckboxGroup<>();
Set<PrintFacade.PrinterDto> selected = printers.getSelectedItems();
for (PrintFacade.PrinterDto p : selected) {
```

---

### 4. **ItemListLayout** - Type Cast Issue (3 errors)
**File**: `src/main/java/com/possable/inventory/ui/ItemListLayout.java`

**Issue**: Return type mismatch in mapItems  

**Fix**: Add proper type handling

---

### 5. **ProfileView** - EntryPointView Import (2 errors)
**File**: `src/main/java/com/possable/view/ProfileView.java`

**Change**:
```java
import com.possable.view.EntryPointView;
```

**To**:
```java
import com.possable.infrastructure.ui.EntryPointView;
```

---

## Quick Fix Script

```bash
# Fix imports in Print Module Service
# Manual edit required - remove old model/repository/service imports

# Fix Order Detail Component  
# Manual edit - line 25

# Fix Order View
# Manual edit - lines 187, 195, 204

# Fix ItemListLayout
# Already fixed in last edit

# Fix ProfileView import
# Replace old view package with infrastructure.ui
```

---

## After Fixes

Expected: **0 compilation errors** ✅  
Then: Run tests and application 