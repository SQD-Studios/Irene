package net.chamosmp.irene;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("UnstableApiUsage")
public class IreneLoader implements PluginLoader {
    @Override
    public void classloader(@NonNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver mavenRepository = new MavenLibraryResolver();
        mavenRepository.addRepository(new RemoteRepository.Builder("mavenCentralGoogleCopy", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());

        mavenRepository.addDependency(new Dependency(new DefaultArtifact("io.lettuce:lettuce-core:7.7.0.RELEASE"), null));

        classpathBuilder.addLibrary(mavenRepository);
    }
}
