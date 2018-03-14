package zone.dragon.dropwizard.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.hk2.api.InterceptionService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Jersey {@link Feature} that enables support for resources that return {@link CompletionStage} or {@link CompletableFuture}
 *
 * @author Bryan Harclerode
 */
public class AsyncFeature implements Feature {
    @Override
    public boolean configure(FeatureContext context) {
        context.register(CompletionStageMessageBodyWriter.class);
        context.register(ListenableFutureMessageBodyWriter.class);
        context.register(RepackagedListenableFutureMessageBodyWriter.class);

        context.register(AsyncModelProcessor.class);
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(AsyncInterceptionService.class).to(InterceptionService.class).in(Singleton.class);
            }
        });
        return true;
    }
}
