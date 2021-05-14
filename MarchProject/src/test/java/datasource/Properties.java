package datasource;

import graphql.TestClass;

import java.io.IOException;
import java.util.HashMap;

public class Properties {

    static private final HashMap<String, Properties> instances = new HashMap<>();
    java.util.Properties props;

    public String get(String key) {
        return props.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public Object get(Object key) {
        return props.get(key);
    }

    private Properties(String name) throws IOException {
        props = new java.util.Properties();
        props.load(TestClass.class.getClassLoader().getResourceAsStream(name + ".properties"));
    }

    public static Properties getProperties(String propName) throws IOException {
        var instance = instances.get(propName);
        if (instance == null) {
            instance = new Properties(propName);
            instances.put(propName, instance);
        }
        return instance;
    }
}
