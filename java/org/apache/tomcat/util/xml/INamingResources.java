package org.apache.tomcat.util.xml;

public interface INamingResources {

	void removeEnvironment(String name);

	void addEnvironment(ContextEnvironment ce);

	void removeResourceLink(String name);

	void addResourceLink(ContextResourceLink crl);

	void removeResource(String name);

	void addResource(ContextResource cr);

	Object getContainer();

}
