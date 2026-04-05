package company.vk.edu.distrib.compute;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.util.ReflectionUtils;

import java.util.Set;
import java.util.stream.Stream;

public class KVServiceFactoryArgumentsProvider implements ArgumentsProvider {
    private final Set<Class<? extends KVServiceFactory>> factories = Set.of(
            company.vk.edu.distrib.compute.mandesero.KVServiceFactoryImpl.class,
            company.vk.edu.distrib.compute.borodinavalera1996dev.KVServiceFactoryImpl.class
    );

    @Override
    @NonNull
    public Stream<? extends Arguments> provideArguments(
        @NonNull ParameterDeclarations parameters,
        @NonNull ExtensionContext context
    ) {
        return factories.stream()
            .map(ReflectionUtils::newInstance)
            .map(Arguments::of);
    }
}
