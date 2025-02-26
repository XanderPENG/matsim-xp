package freight_collaboration;

import org.matsim.core.controler.AbstractModule;

import javax.inject.Singleton;

public class CollaborationModule extends AbstractModule {

    @Override
    public void install() {
//        bind(CollaboratorControllerListener.class).in(Singleton.class);
        addControlerListenerBinding().to(CollaboratorControllerListener.class);
//        bind(Collaborator.class).in(Singleton.class);
        bind(Collaborators.class).in(Singleton.class);
    }
}
