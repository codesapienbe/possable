package com.possable;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies the Spring Modulith module structure and generates documentation.
 */
class ModuleStructureTest {

    ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifiesModularStructure() {
        // Verify that the application has valid module structure
        modules.verify();
    }

    @Test
    void createModuleDocumentation() throws Exception {
        // Generate module documentation
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }

    @Test
    void printModules() {
        // Print all detected modules
        modules.forEach(module -> {
            System.out.println("Module: " + module.getName());
            System.out.println("  Base Package: " + module.getBasePackage());
            System.out.println("  Dependencies: " + module.getDependencies());
            System.out.println();
        });
    }
} 