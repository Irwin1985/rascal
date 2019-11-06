package org.rascalmpl.uri.jar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.rascalmpl.uri.ISourceLocationInput;
import org.rascalmpl.uri.URIResolverRegistry;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IValueFactory;
import org.rascalmpl.values.ValueFactoryFactory;

public class JarURIResolver implements ISourceLocationInput {
	private static final IValueFactory VF = ValueFactoryFactory.getValueFactory();
	private final JarFileResolver file = new JarFileResolver();
	private final JarFileResolver inputStream;
    private final URIResolverRegistry registry;

	public JarURIResolver(URIResolverRegistry registry) {
	    this.registry = registry;
        inputStream = new JarInputStreamResolver(registry);
	    
    }
	
    @Override
    public String scheme() {
        return "jar";
    }
    
    private JarFileResolver getTargetResolver(ISourceLocation uri) {
       if (uri.getScheme().startsWith("jar+")) {
           return inputStream;
       }
       return file;
    }
    
    private ISourceLocation safeResolve(ISourceLocation loc) {
        try {
            return registry.logicalToPhysical(loc);
        }
        catch (Throwable e) {
            return loc;
        }
    }
    
    private static String getInsideJarPath(ISourceLocation uri) {
        String path = uri.getPath();
        if (path != null && !path.isEmpty()) {
            int bang = path.lastIndexOf('!');
            if (bang != -1) {
                path = path.substring(bang + 1);
                while (path.startsWith("/")) { 
                    path = path.substring(1);
                }
                return path;
            }
        }
        return "";
    }

    private ISourceLocation getResolvedJarPath(ISourceLocation uri) throws IOException {
        boolean isWrapped = uri.getScheme().startsWith("jar+");
        try {
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                int bang = path.lastIndexOf('!');
                if (bang != -1) {
                    return safeResolve(VF.sourceLocation(
                        isWrapped ? uri.getScheme().substring("jar+".length()) : "file",
                        isWrapped ? uri.getAuthority() : "",
                        path.substring(path.indexOf("/"), bang)));
                }
            }
            throw new IOException("The jar and the internal path should be separated with a ! (" + uri.getPath() + ")");
        }
        catch (UnsupportedOperationException | URISyntaxException e) {
			throw new IOException("Invalid URI: \"" + uri +"\"", e);
        }
    }
    
    @Override
    public InputStream getInputStream(ISourceLocation uri) throws IOException {
        ISourceLocation jarUri = getResolvedJarPath(uri);
        return getTargetResolver(jarUri).getInputStream(jarUri, getInsideJarPath(uri));
    }


    @Override
    public boolean isDirectory(ISourceLocation uri) {
        if (uri.getPath() != null && (uri.getPath().endsWith("!") || uri.getPath().endsWith("!/"))) {
            // if the uri is the root of a jar, and it ends with a ![/], it should be considered a
            // directory
            return true;
        }
        try {
            ISourceLocation jarUri = getResolvedJarPath(uri);
            return getTargetResolver(jarUri).isDirectory(jarUri, getInsideJarPath(uri));
        }
        catch (IOException e) {
            return false;
        }
    }
    
    
    @Override
    public boolean exists(ISourceLocation uri) {
        try {
            ISourceLocation jarUri = getResolvedJarPath(uri);
            return getTargetResolver(jarUri).exists(jarUri, getInsideJarPath(uri));
        }
        catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public boolean isFile(ISourceLocation uri) {
        try {
            ISourceLocation jarUri = getResolvedJarPath(uri);
            return getTargetResolver(jarUri).isFile(jarUri, getInsideJarPath(uri));
        }
        catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public Charset getCharset(ISourceLocation uri) throws IOException {
        return null; // one day we might read the meta-inf?
    }
    
    @Override
    public long lastModified(ISourceLocation uri) throws IOException {
        ISourceLocation jarUri = getResolvedJarPath(uri);
        return getTargetResolver(jarUri).lastModified(jarUri, getInsideJarPath(uri));
    }
    
    @Override
    public String[] list(ISourceLocation uri) throws IOException {
        ISourceLocation jarUri = getResolvedJarPath(uri);
        return getTargetResolver(jarUri).list(jarUri, getInsideJarPath(uri));
    }

    
    @Override
    public boolean supportsHost() {
        return true; // someone we wrap might support host
    }
}
