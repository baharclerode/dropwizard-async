package zone.dragon.dropwizard.async;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * @author Darth Android
 * @date 3/13/2018
 */
@javax.ws.rs.ext.Provider
public class CompletionStageMessageBodyWriter extends UnwrappingMessageBodyWriter<CompletionStage<?>> {

    @Inject
    public CompletionStageMessageBodyWriter(Provider<MessageBodyWorkers> mbwProvider) {
        super(mbwProvider, CompletionStage.class, 0, value -> value.toCompletableFuture().getNow(null));
    }

}
