package org.apache.tomcat.util.xml;

/**
 * Holds and manages the naming resources defined in the J2EE Enterprise Naming
 * Context and their associated JNDI context.
 */
public interface INamingResources {

    /**
     * Remove any environment entry with the specified name.
     * 
     * @param name Name of the environment entry to remove
     */
    void removeEnvironment(String name);

    /**
     * Add an environment entry for this web application.
     * 
     * @param environment New environment entry
     */
    void addEnvironment(ContextEnvironment ce);

    /**
     * Remove any resource link with the specified name.
     * 
     * @param name Name of the resource link to remove
     */
    void removeResourceLink(String name);

    /**
     * Add a resource link for this web application.
     * 
     * @param resourceLink New resource link
     */
    void addResourceLink(ContextResourceLink crl);

    /**
     * Remove any resource reference with the specified name.
     * 
     * @param name Name of the resource reference to remove
     */
    void removeResource(String name);

    /**
     * Add a resource reference for this web application.
     * 
     * @param resource New resource reference
     */
    void addResource(ContextResource cr);

    /**
     * Get the container with which the naming resources are associated.
     */
    Object getContainer();

}
