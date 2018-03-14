package zone.dragon.dropwizard.async;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.inject.Provider;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.message.MessageBodyWorkers;

import lombok.NonNull;

/**
 * @author Darth Android
 * @date 3/12/2018
 */
@Produces(MediaType.WILDCARD)
public abstract class UnwrappingMessageBodyWriter<T> implements MessageBodyWriter<T> {


    private final Provider<MessageBodyWorkers> writersProvider;

    private final Class<? super T> baseClass;

    private final int baseClassVariableIndex;

    private final Function<T, Object> unwrappingExtractor;

    public UnwrappingMessageBodyWriter(@NonNull Provider<MessageBodyWorkers> writersProvider, @NonNull Class<? super T> baseClass, int baseClassVariableIndex, @NonNull Function<T, Object> unwrappingExtractor) {
        this.writersProvider = writersProvider;
        this.baseClass = baseClass;
        this.baseClassVariableIndex = baseClassVariableIndex;
        this.unwrappingExtractor = unwrappingExtractor;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return baseClass.isAssignableFrom(type) &&
            getWriter(type, genericType, annotations, mediaType) != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public long getSize(T value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        MessageBodyWriter<T> writer = getWriter(type, genericType, annotations, mediaType);
        if (writer == null) {
            return 0;
        }
        return writer.getSize(value, type, genericType, annotations, mediaType);
    }


    @Override
    public void writeTo(T value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        MessageBodyWriter<Object> writer = getWriter(type, genericType, annotations, mediaType);
        if (writer == null) {
            throw new NullPointerException("Could not find message body writer for " + type);
        }
        Object unwrappedValue = value;
        if (value != null) {
            unwrappedValue = unwrappingExtractor.apply(value);
        }
        writer.writeTo(unwrappedValue, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    @SuppressWarnings("unchecked")
    protected <U> MessageBodyWriter<U> getWriter(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (CompletionStage.class.isAssignableFrom(ReflectionHelper.erasure(genericType))) {
            genericType = resolveType(genericType, baseClass, baseClassVariableIndex);
            type = ReflectionHelper.erasure(genericType);
        } else if (baseClass.isAssignableFrom(type)) {
            genericType = resolveType(type, baseClass, baseClassVariableIndex);
            type = ReflectionHelper.erasure(genericType);
        } else {
            genericType = Object.class;
            type = Object.class;
        }
        return (MessageBodyWriter) writersProvider.get().getMessageBodyWriter(type, genericType, annotations, mediaType);
    }

    protected final Type resolveType(Type boundType, Class targetClass, int targetClassIndex) {
        return resolveType(boundType, targetClass.getTypeParameters()[targetClassIndex]);
    }

    protected final Type resolveType(Type boundType, TypeVariable targetType) {
        List<Type> typeQueue = new ArrayList<>(1);
        typeQueue.add(boundType);
        Map<TypeVariable, Type> knownTypes = new HashMap<>();
        while (!knownTypes.containsKey(targetType)) {
            if (typeQueue.isEmpty()) {
                return null;
            }
            Type next = typeQueue.remove(0);
            if (next instanceof ParameterizedType) {
                Class<?> rawClass = ReflectionHelper.erasure(next);
                TypeVariable<?>[] boundVariables = rawClass.getTypeParameters();
                for (int i = 0; i < boundVariables.length; i++) {
                    knownTypes.putIfAbsent(boundVariables[i], ((ParameterizedType) next).getActualTypeArguments()[i]);
                }
            } else if (next instanceof Class) {
                for (Type interfaceType : ((Class) next).getGenericInterfaces()) {
                    typeQueue.add(interfaceType);
                }
                typeQueue.add(((Class) next).getGenericSuperclass());
            }
        }
        return knownTypes.get(targetType);
    }


}
