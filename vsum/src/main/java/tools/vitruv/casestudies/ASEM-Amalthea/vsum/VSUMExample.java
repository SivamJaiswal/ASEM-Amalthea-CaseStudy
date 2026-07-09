package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;

import org.eclipse.app4mc.amalthea.model.Amalthea;
import org.eclipse.app4mc.amalthea.model.AmaltheaFactory;
import org.eclipse.app4mc.amalthea.model.Component;

import mir.reactions.amaltheaToAsem.AmaltheaToAsemChangePropagationSpecification;
import mir.reactions.asemToAmalthea.AsemToAmaltheaChangePropagationSpecification;

import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/** This class provides an example how to define and use a VSUM. */
public class VSUMExample {
  private static final String AMALTHEA_FILE = "/amalthea.amxmi";

  public static void main(String[] args) throws IOException {
    Path storageFolder = Path.of("vsumexample");
    VirtualModel vsum = createDefaultVirtualModel(storageFolder);

    // Register the Amalthea root; a corresponding ASEM Module is created for
    // every Component added underneath it (see AmaltheaToAsem.reactions).
    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          Amalthea root = AmaltheaFactory.eINSTANCE.createAmalthea();
          root.setComponentsModel(AmaltheaFactory.eINSTANCE.createComponentsModel());
          root.setSwModel(AmaltheaFactory.eINSTANCE.createSWModel());
          v.registerRoot(root, URI.createFileURI(storageFolder + AMALTHEA_FILE));
        });

    modifyView(
        getDefaultView(vsum).withChangeRecordingTrait(),
        (CommittableView v) -> {
          Amalthea root = v.getRootObjects(Amalthea.class).iterator().next();
          Component component = AmaltheaFactory.eINSTANCE.createComponent();
          component.setName("ExampleComponent");
          root.getComponentsModel().getComponents().add(component);
        });
  }

  private static VirtualModel createDefaultVirtualModel(Path storageFolder) throws IOException {
    Iterable<ChangePropagationSpecification> specs =
        List.of(
            new AmaltheaToAsemChangePropagationSpecification(),
            new AsemToAmaltheaChangePropagationSpecification());
    return new VirtualModelBuilder()
        .withStorageFolder(storageFolder)
        .withUserInteractorForResultProvider(
            new TestUserInteraction.ResultProvider(new TestUserInteraction()))
        .withChangePropagationSpecifications(specs)
        .buildAndInitialize();
  }

  private static View getDefaultView(VirtualModel vsum) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private static void modifyView(
      CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    view.commitChanges();
  }
}
