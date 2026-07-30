package tools.vitruv.methodologist.template.vsum;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.*;

/**
 * Tests Completeness invariants (C1-C11) where changes originate on the ASEM side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AsemToAmaltheaCompletenessTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }
}
