package zone.dragon.dropwizard.async;

import java.lang.reflect.Type;
import java.util.Observable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Configuration;

import org.glassfish.hk2.utilities.reflection.ReflectionHelper;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;

import com.google.common.util.concurrent.ListenableFuture;

import lombok.extern.slf4j.Slf4j;

/**
 * Model Processor to alter resource methods to run {@link Suspended} if they return a {@link ListenableFuture}, {@link Observable},
 * {@link CompletionStage}, and {@link CompletableFuture}
 *
 * @author Bryan Harclerode
 */
@Slf4j
@Singleton
public class AsyncModelProcessor implements ModelProcessor {
    private ResourceModel processModel(ResourceModel originalModel, boolean subresource) {
        ResourceModel.Builder modelBuilder = new ResourceModel.Builder(subresource);
        for (Resource originalResource : originalModel.getResources()) {
            modelBuilder.addResource(updateResource(originalResource));
        }
        return modelBuilder.build();
    }

    @Override
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
        return processModel(resourceModel, false);
    }

    @Override
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
        return processModel(subResourceModel, true);
    }

    protected boolean isAsyncType(Type type) {
        Class<?> rawType = ReflectionHelper.getRawClass(type);
        return CompletionStage.class.isAssignableFrom(rawType)
               || ListenableFuture.class.isAssignableFrom(rawType)
               || Observable.class.isAssignableFrom(rawType);
    }

    private Resource updateResource(Resource original) {
        // replace all methods on this resource, and then recursively repeat upon all child resources
        Resource.Builder resourceBuilder = Resource.builder(original);
        for (Resource childResource : original.getChildResources()) {
            resourceBuilder.replaceChildResource(childResource, updateResource(childResource));
        }
        for (ResourceMethod originalMethod : original.getResourceMethods()) {
            if (isAsyncType(originalMethod.getInvocable().getRawRoutingResponseType())) {
                log.info("Marking resource method as suspended: {}", originalMethod.getInvocable().getRawRoutingResponseType());
                resourceBuilder.updateMethod(originalMethod).suspended(AsyncResponse.NO_TIMEOUT, TimeUnit.MILLISECONDS);
            }
        }
        return resourceBuilder.build();
    }
}
