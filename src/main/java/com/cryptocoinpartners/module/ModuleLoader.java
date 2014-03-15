package com.cryptocoinpartners.module;

import com.cryptocoinpartners.service.Esper;
import com.cryptocoinpartners.util.ModuleLoaderError;
import com.cryptocoinpartners.util.ReflectionUtil;
import org.apache.commons.configuration.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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
 * <li>Any class which has a *.epl files are loaded as Esper Event Processing Language source</li>
 * </ol>
 *
 * @author Tim Olson
 * @see ModuleListener
 */
public class ModuleLoader {


    public static void load(Esper esper, String... moduleNames) throws ModuleLoaderError {
        try {
            init();
            for( String name : moduleNames ) {
                if( esper.isModuleLoaded(name) ) {
                    log.debug("skipping loaded module " + name);
                    break;
                }
                log.info("loading module "+name);
                Configuration config = getConfig(name);
                Collection<ModuleListener> lifecycles = initListenerClasses(esper,name);
                for( ModuleListener lifecycle : lifecycles )
                    lifecycle.initModule(esper,config);
                loadEsperFiles(esper, name);
            }
        }
        catch( Throwable e ) {
            throw new ModuleLoaderError(e);
        }
    }


    private static Configuration getConfig(String name) throws ConfigurationException {
        CompositeConfiguration config = new CompositeConfiguration();
        String path = "com/cryptocoinpartners/module/" + name;
        File[] files = new File(path).listFiles();
        if( files != null ) {
            for( File file : files ) {
                if( file.getName().toLowerCase().matches("config\\.[^\\.]") ) {
                    log.debug("loading config file "+file.getPath());
                    Configuration moreConfig = new ConfigurationFactory(file.getPath()).getConfiguration();
                    config.addConfiguration(moreConfig);
                }
            }
        }
        log.debug("module configuration is\n"+config);
        return config;
    }


    private static Collection<ModuleListener> initListenerClasses(Esper esper, String name)
            throws IllegalAccessException, InstantiationException {
        Collection<Class<? extends ModuleListener>> listenerClasses = moduleListenerClasses.get(name);
        ArrayList<ModuleListener> listeners = new ArrayList<ModuleListener>();
        for( Class<? extends ModuleListener> listenerClass : listenerClasses ) {
            log.debug("instantiating ModuleListener "+listenerClass.getSimpleName());
            ModuleListener listener = listenerClass.newInstance();
            listeners.add(listener);
            initTriggers(esper,listener);
        }
        return listeners;
    }


    private static void initTriggers(Esper esper, ModuleListener listener) {
        for( Method method : listener.getClass().getMethods() ) {
            When when = method.getAnnotation(When.class);
            if( when != null ) {
                String statement = when.value();
                log.debug("subscribing "+method+" with statement \""+statement+"\"");
                esper.subscribe(listener, method, statement);
            }
        }

    }


    private static void load(Esper esper, File file) throws IOException {
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
