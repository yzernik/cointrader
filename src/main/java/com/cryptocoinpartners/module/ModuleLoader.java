package com.cryptocoinpartners.module;

import com.cryptocoinpartners.util.*;
import com.espertech.esper.client.deploy.DeploymentException;
import com.espertech.esper.client.deploy.ParseException;
import org.apache.commons.configuration.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Modules are subpackages of com.cryptocoinpartners.module, and any files/classes in this package are loaded/executed
 * according to their type:
 * <ol>
 * <li>Any files named conf.* are loaded into an Apache Commons Configuration object</li>
 * <li>Classes implementing @ModuleListener have singletons created with their default constructor, and lifecycle
 * notifications are sent to those instances.</li>
 * <li>*.epl files are loaded as Esper Event Processing Language source</li>
 * <li>Any file named *.epl is loaded as Esper Event Processing Language source.  If the file has the same name as a
 * Java ModuleListener in the same module, then the EPL file may use the @IntoMethod annotation on its esper statements
 * to push results into the module listener singleton's fields.</li>
 * </ol>
 *
 * @author Tim Olson
 * @see ModuleListener
 * @see IntoMethod
 * @see When
 */
public class ModuleLoader {

    public static void load(Esper esper, String... moduleNames) throws ModuleLoaderError {
        load(esper, null, moduleNames);
    }


    public static void load(Esper esper, @Nullable AbstractConfiguration config, String... moduleNames) throws ModuleLoaderError {
        try {
            init();
            for( String name : moduleNames ) {
                if( esper.isModuleLoaded(name) ) {
                    log.debug("skipping loaded module " + name);
                    break;
                }
                log.info("loading module "+name);
                Configuration fullConfiguration = buildConfig(name,config);
                Collection<ModuleListener> lifecycles = initListenerClasses(esper,name);
                for( ModuleListener lifecycle : lifecycles )
                    lifecycle.initModule(esper,fullConfiguration);
                loadEsperFiles(esper, name);
            }
        }
        catch( Throwable e ) {
            throw new ModuleLoaderError(e);
        }
    }


    private static AbstractConfiguration buildConfig(String name, @Nullable AbstractConfiguration c)
            throws ConfigurationException {
        final ClassLoader classLoader = ModuleLoader.class.getClassLoader();
        final ArrayList<AbstractConfiguration> moduleConfigs = new ArrayList<AbstractConfiguration>();

        // first priority is the caller's configuration
        if( c != null )
            moduleConfigs.add(c);

        // then add the package-specific props file
        String packageName = "com/cryptocoinpartners/module/"+name+"/"+name+".properties";
        URL resource = classLoader.getResource(packageName);
        if (resource != null) {
            PropertiesConfiguration packageConfig = new PropertiesConfiguration(resource);
            moduleConfigs.add(packageConfig);
        }

        // then the more generic config.properties
        packageName = "com/cryptocoinpartners/module/"+name+"/config.properties";
        resource = classLoader.getResource(packageName);
        if (resource != null) {
            PropertiesConfiguration packageConfig = new PropertiesConfiguration(resource);
            moduleConfigs.add(packageConfig);
        }

        return Config.module(moduleConfigs);
    }


    private static Collection<ModuleListener> initListenerClasses(Esper esper, String name)
            throws IllegalAccessException, InstantiationException {
        Collection<Class<? extends ModuleListener>> listenerClasses = moduleListenerClasses.get(name);
        ArrayList<ModuleListener> listeners = new ArrayList<ModuleListener>();
        for( Class<? extends ModuleListener> listenerClass : listenerClasses ) {
            log.debug("instantiating ModuleListener "+listenerClass.getSimpleName());
            ModuleListener listener = listenerClass.newInstance();
            listeners.add(listener);
            esper.subscribe(listener);
        }
        return listeners;
    }


    private static void load(Esper esper, File file) throws IOException, ParseException, DeploymentException {
        esper.loadStatements(FileUtils.readFileToString(file));
    }


    private static void loadEsperFiles(Esper esper, String name) throws Exception {
        String path = "com/cryptocoinpartners/module/" + name;
        File[] files = new File(path).listFiles();
        if( files != null ) {
            for( File file : files ) {
                if( file.getName().toLowerCase().endsWith(".epl") ) {
                    log.debug("loading epl file "+file.getName());
                    load(esper, file);
                }
            }
        }
    }


    static void init() throws IllegalAccessException, InstantiationException {
        if( moduleListenerClasses != null )
            return;
        moduleListenerClasses = new HashMap<String, Collection<Class<? extends ModuleListener>>>();
        Pattern pattern = Pattern.compile("com\\.cryptocoinpartners\\.module\\.([^\\.]+)\\..+");
        Set<Class<? extends ModuleListener>> subs = ReflectionUtil.getSubtypesOf(ModuleListener.class);
        for( Class<? extends ModuleListener> subclass : subs ) {
            if( subclass.equals(ModuleListenerBase.class) )
                continue;
            Matcher matcher = pattern.matcher(subclass.getName());
            if( !matcher.matches() ) {
                log.warn("ignoring "+subclass.getName()+" because it is not in a subpackage of com.cryptocoinpartners.module");
            }
            else {
                String moduleName = matcher.group(1);
                Collection<Class<? extends ModuleListener>> listeners = moduleListenerClasses.get(moduleName);
                if( listeners == null ) {
                    listeners = new ArrayList<Class<? extends ModuleListener>>();
                    moduleListenerClasses.put(moduleName, listeners);
                }
                listeners.add(subclass);
            }
        }
    }


    private static Logger log = LoggerFactory.getLogger(ModuleLoader.class);
    private static Map<String,Collection<Class<? extends ModuleListener>>> moduleListenerClasses;
}
