package tools.vitruv.methodologist.template.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.eclipse.app4mc.amalthea.model.BaseTypeDefinition;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.Module;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.Message;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.PrimitiveTypeRepository;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests Completeness invariants (C1-C11) where changes originate on the AMALTHEA side.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class AmaltheaToAsemCompletenessTest {

    VSUMRunner util = new VSUMRunner();

    @BeforeAll
    static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());
    }

    // Rule 1 footnote invariants

    @Test
    @DisplayName("R1** – All Methods in Module must have null returnType and no parameters")
    void r1_moduleMethodsMustBeVoid(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "ECM");
        util.addRunnable(vsum, "ECM", "runA");
        util.addRunnable(vsum, "ECM", "runB");

        Module module = util.getCorrespondingInAsem(vsum, "ECM", Module.class);
        module.getMethods().forEach(m -> {
            assertNull(m.getReturnType(), "returnType must be null (footnote **)");
            assertTrue(m.getParameters().isEmpty(), "no parameters (footnote **)");
        });
    }

    @Test
    @DisplayName("R1* – All typedElements in Module must be Message instances")
    void r1_moduleTypedElementsMustBeMessages(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);

        util.addComponent(vsum, "SensorHub");
        util.addLabel(vsum, "SensorHub", "signal1", false);
        util.addLabel(vsum, "SensorHub", "signal2", false);

        Module module = util.getCorrespondingInAsem(vsum, "SensorHub", Module.class);
        module.getTypedElements().forEach(te ->
                assertInstanceOf(Message.class, te,
                        "all typedElements must be Message instances (footnote *)"));
    }

    // C11 — Exactly one PrimitiveTypeRepository exists per model

    @Test
    @DisplayName("C11 – PrimitiveTypeRepository is a singleton, not one per BaseTypeDefinition")
    void c11_primitiveTypeRepositoryIsSingleton(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "uint8", 8, "u8");
        util.addBaseTypeDefinition(vsum, "double64", 64, "double64");

        long repoCount = util.getDefaultView(vsum, java.util.List.of(PrimitiveTypeRepository.class))
                .getRootObjects(PrimitiveTypeRepository.class).size();
        assertEquals(1, repoCount, "only one PrimitiveTypeRepository must exist, regardless of how many types get created");
    }

    @Test
    @DisplayName("C11 – PrimitiveTypeRepository survives a BaseTypeDefinition being created and then deleted")
    void c11_primitiveTypeRepository_survivesBaseTypeDefinitionDelete(@TempDir Path tempDir) throws Exception {
        InternalVirtualModel vsum = util.createDefaultVirtualModel(tempDir);
        util.registerRootObjects(vsum, tempDir);
        util.addBaseTypeDefinition(vsum, "temp_x", 8, "u8");
        assertNotNull(util.getPrimitiveTypeRepository(vsum));

        util.deleteFromAmalthea(vsum, "temp_x", BaseTypeDefinition.class);

        PrimitiveTypeRepository repo = util.getPrimitiveTypeRepository(vsum);
        assertNotNull(repo, "the repository must still exist after the BaseTypeDefinition is deleted");
        assertEquals("PrimitiveTypes", repo.getName());
    }
}
