package zone.dragon.dropwizard.async;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.message.MessageBodyWorkers;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author Darth Android
 * @date 3/13/2018
 */
@javax.ws.rs.ext.Provider
public class ListenableFutureMessageBodyWriter extends UnwrappingMessageBodyWriter<ListenableFuture<?>> {

    @Inject
    public ListenableFutureMessageBodyWriter(Provider<MessageBodyWorkers> mbwProvider) {
        super(mbwProvider, ListenableFuture.class, 0, value -> {
            try {
                return value.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

}
