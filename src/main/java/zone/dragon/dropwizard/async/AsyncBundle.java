package zone.dragon.dropwizard.async;

import java.util.Observable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.google.common.util.concurrent.ListenableFuture;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Configures DropWizard to support returning {@link ListenableFuture}, {@link Observable}, {@link CompletionStage}, and
 * {@link CompletableFuture} from resource methods
 *
 * @author Bryan Harclerode
 */
public class AsyncBundle implements Bundle {
    @Override
    public void initialize(Bootstrap<?> bootstrap) { }

    @Override
    public void run(Environment environment) {
        environment.jersey().register(AsyncFeature.class);
    }
}
