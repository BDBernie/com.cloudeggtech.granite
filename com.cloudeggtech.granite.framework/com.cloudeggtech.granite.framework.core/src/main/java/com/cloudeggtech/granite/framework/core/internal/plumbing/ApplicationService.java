package com.cloudeggtech.granite.framework.core.internal.plumbing;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudeggtech.granite.framework.core.IApplication;
import com.cloudeggtech.granite.framework.core.annotations.AppComponent;
import com.cloudeggtech.granite.framework.core.annotations.Dependency;
import com.cloudeggtech.granite.framework.core.commons.osgi.IBundleContextAware;
import com.cloudeggtech.granite.framework.core.commons.utils.ContributionClass;
import com.cloudeggtech.granite.framework.core.commons.utils.ContributionClassTrackHelper;
import com.cloudeggtech.granite.framework.core.commons.utils.IContributionClassTracker;
import com.cloudeggtech.granite.framework.core.config.IApplicationConfiguration;
import com.cloudeggtech.granite.framework.core.config.IApplicationConfigurationAware;
import com.cloudeggtech.granite.framework.core.config.IConfigurationAware;
import com.cloudeggtech.granite.framework.core.plumbing.ApplicationComponentConfigurations;
import com.cloudeggtech.granite.framework.core.plumbing.IApplicationComponentConfigurations;
import com.cloudeggtech.granite.framework.core.plumbing.IApplicationComponentService;
import com.cloudeggtech.granite.framework.core.plumbing.IApplicationComponentServiceAware;
import com.cloudeggtech.granite.framework.core.plumbing.persistent.IPersistentObjectFactory;
import com.cloudeggtech.granite.framework.core.plumbing.persistent.IPersistentObjectFactoryAware;
import com.cloudeggtech.granite.framework.core.repository.CreationException;
import com.cloudeggtech.granite.framework.core.repository.IComponentCollector;
import com.cloudeggtech.granite.framework.core.repository.IComponentInfo;
import com.cloudeggtech.granite.framework.core.repository.IDependencyInfo;
import com.cloudeggtech.granite.framework.core.repository.IInitializable;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

public class ApplicationService implements IComponentCollector, IApplicationComponentService,
		IContributionClassTracker<AppComponent> {
	private static final String KEY_GRANITE_APPLICATION_COMPONENT_SCAN = "Granite-Application-Component-Scan";
	private static final String KEY_GRANITE_APPLICATION_COMPONENT_SCAN_PATHS = "Granite-Application-Component-Scan-Paths";
	private static final String ID_APPLICATION_COMPONENT_CONFIGURATIONS = "application.component.configurations";
	
	private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);
	
	private ServiceRegistration<IComponentCollector> srComponentCollector;
	private ServiceRegistration<IApplicationComponentService> srAppComponentService;
	
	private ConcurrentMap<String, IComponentInfo> components;
	
	private ConcurrentMap<Class<?>, List<DependencyInjector>> dependencyInjectors;
	
	private ContributionClassTrackHelper<AppComponent> trackHelper;
	
	private IApplicationConfiguration appConfiguration;
	private BundleContext bundleContext;
	
	private PersistentObjectFactory persistentObjectFactory;
	private ServiceTracker<IPersistentObjectFactory, IPersistentObjectFactory> persistentObjectFactoryTracker;
	
	private IApplicationComponentConfigurations appComponentConfigurations;
	
	public ApplicationService(BundleContext bundleContext, IApplicationConfiguration appConfiguration) {
		this.bundleContext = bundleContext;
		this.appConfiguration = appConfiguration;
	}

	public void start() throws Exception {
		components = new ConcurrentHashMap<>();
		
		dependencyInjectors = new ConcurrentHashMap<>();
		
		appComponentConfigurations = new ApplicationComponentConfigurations(appConfiguration.getConfigDir());
		
		trackPersistentObjectFactory();
		
		componentFound(new AppComponentConfigurationsComponentInfo(appComponentConfigurations));
		
		trackHelper = new ContributionClassTrackHelper<>(bundleContext,
				KEY_GRANITE_APPLICATION_COMPONENT_SCAN, KEY_GRANITE_APPLICATION_COMPONENT_SCAN_PATHS,
					AppComponent.class, this);
		trackHelper.track();
		
		exportOsgiServices();
	}

	private void exportOsgiServices() {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_INTENTS, IApplication.GRANITE_APP_COMPONENT_COLLECTOR);
		srComponentCollector = bundleContext.registerService(IComponentCollector.class, this, properties);
		
		srAppComponentService = bundleContext.registerService(IApplicationComponentService.class, this, null);
	}
	
	private void trackPersistentObjectFactory() {
		persistentObjectFactory = new PersistentObjectFactory();
		
		persistentObjectFactoryTracker = new ServiceTracker<>(bundleContext, IPersistentObjectFactory.class,
				new ServiceTrackerCustomizer<IPersistentObjectFactory, IPersistentObjectFactory>() {
			@Override
			public IPersistentObjectFactory addingService(ServiceReference<IPersistentObjectFactory> reference) {
				IPersistentObjectFactory impl = bundleContext.getService(reference);
				persistentObjectFactory.setImpl(impl);
				
				return impl;
			}

			@Override
			public void modifiedService(ServiceReference<IPersistentObjectFactory> reference,
					IPersistentObjectFactory service) {}

			@Override
			public void removedService(ServiceReference<IPersistentObjectFactory> reference,
					IPersistentObjectFactory service) {
				persistentObjectFactory.setImpl(null);
			}
		});
		
		persistentObjectFactoryTracker.open();
	}

	private class PersistentObjectFactory implements IPersistentObjectFactory {
		private volatile IPersistentObjectFactory impl;

		@SuppressWarnings("unchecked")
		@Override
		public <K, V extends K> V create(Class<K> clazz) {
			if (impl == null) {
				try {
					return (V)clazz.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(String.format("Can't initialize persistent object.", clazz.getName()), e);
				}
			}
			
			return impl.create(clazz);
		}
		
		public void setImpl(IPersistentObjectFactory impl) {
			this.impl = impl;
		}
	}
	
	private class AppComponentConfigurationsComponentInfo implements IComponentInfo {
		private IApplicationComponentConfigurations appComponentConfigurations;
		
		public AppComponentConfigurationsComponentInfo(IApplicationComponentConfigurations appComponentConfigurations) {
			this.appComponentConfigurations = appComponentConfigurations;
		}

		@Override
		public String getId() {
			return ID_APPLICATION_COMPONENT_CONFIGURATIONS;
		}

		@Override
		public void addDependency(IDependencyInfo dependency) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeDependency(IDependencyInfo dependency) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IDependencyInfo[] getDependencies() {
			return new IDependencyInfo[0];
		}

		@Override
		public boolean isAvailable() {
			return true;
		}

		@Override
		public boolean isService() {
			return false;
		}

		@Override
		public Object create() throws CreationException {
			return appComponentConfigurations;
		}

		@Override
		public BundleContext getBundleContext() {
			return bundleContext;
		}

		@Override
		public IComponentInfo getAliasComponent(String alias) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
		
	}

	public void stop() throws Exception {
		srAppComponentService.unregister();
		srComponentCollector.unregister();
		
		persistentObjectFactoryTracker.close();
	}

	@Override
	public void componentFound(IComponentInfo componentInfo) {
		components.put(componentInfo.getId(), componentInfo);
		if (componentInfo instanceof IApplicationComponentServiceAware) {
			((IApplicationComponentServiceAware)componentInfo).setApplicationComponentService(this);
		}
		logger.debug("application component {} found", componentInfo);
	}

	@Override
	public void componentLost(String componentId) {
		components.remove(componentId);
		logger.debug("Application component {} lost.", componentId);
	}

	@Override
	public void inject(Object object, BundleContext bundleContext) {
		Class<?> clazz = object.getClass();
		
		for (DependencyInjector injector : getDependencyInjectors(clazz)) {
			injector.inject(object);
		}
		
		if (object instanceof IApplicationConfigurationAware) {
			((IApplicationConfigurationAware)object).setApplicationConfiguration(appConfiguration);
		}
		
		if (object instanceof IConfigurationAware) {
			((IConfigurationAware)object).setConfiguration(appComponentConfigurations.getConfiguration(
					bundleContext.getBundle().getSymbolicName()));
		}
		
		if (object instanceof IBundleContextAware) {
			((IBundleContextAware)object).setBundleContext(bundleContext);
		}
		
		if (object instanceof IPersistentObjectFactoryAware) {
			((IPersistentObjectFactoryAware)object).setPersistentObjectFactory(persistentObjectFactory);
		}
		
		if (object instanceof IInitializable) {
			((IInitializable)object).init();
		}
	}
	
	private List<DependencyInjector> getDependencyInjectors(Class<?> clazz) {
		List<DependencyInjector> injectors = dependencyInjectors.get(clazz);
		
		if (injectors != null)
			return injectors;
		
		injectors = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			Dependency dependencyAnnotation = field.getAnnotation(Dependency.class);
			if (dependencyAnnotation == null) {
				continue;
			}
			
			injectors.add(new FieldDependencyInjector(field, dependencyAnnotation.value()));			
		}
		
		for (Method method : clazz.getMethods()) {
			if (method.getDeclaringClass().equals(Object.class))
				continue;
			
			int modifiers = method.getModifiers();
			if (!Modifier.isPublic(modifiers) ||
					Modifier.isAbstract(modifiers) ||
					Modifier.isStatic(modifiers))
				continue;
			
			Dependency dependencyAnnotation = method.getAnnotation(Dependency.class);
			if (dependencyAnnotation == null) {
				continue;
			}
			
			if (!isSetterMethod(method)) {
				logger.warn(String.format("Dependency method '%s' isn't a setter method.", method));
				continue;
			}
			
			injectors.add(new MethodDependencyInjector(method, dependencyAnnotation.value()));
		}
		
		List<DependencyInjector> old = dependencyInjectors.putIfAbsent(clazz, injectors);
		
		return old == null ? injectors : old;
	}

	private interface DependencyInjector {
		void inject(Object object);
	}
	
	private class FieldDependencyInjector implements DependencyInjector {
		private Field field;
		private String componentId;
		
		public FieldDependencyInjector(Field field, String componentId) {
			this.field = field;
			this.componentId = componentId;
		}

		@Override
		public void inject(Object object) {
			Object component = null;
			try {
				component = getComponent(componentId, field.getType());
			} catch (Exception e) {
				throw new RuntimeException(String.format("Can't get dependency component '%s'.", componentId), e);
			}
			
			boolean oldAccessible = field.isAccessible();
			field.setAccessible(true);
			try {
				field.set(object, component);
			} catch (Exception e) {
				throw new RuntimeException(String.format("Can't inject dependency '%s'.", componentId), e);
			} finally {
				field.setAccessible(oldAccessible);
			}
		}
		
	}
	
	private class MethodDependencyInjector implements DependencyInjector {
		private Method method;
		private String componentId;
		
		public MethodDependencyInjector(Method method, String componentId) {
			this.method = method;
			this.componentId = componentId;
		}

		@Override
		public void inject(Object object) {
			if (isSetterMethod(method)) {
				Object component = null;
				try {
					component = getComponent(componentId, method.getParameterTypes()[0]);
				} catch (Exception e) {
					throw new RuntimeException(String.format("Can't get dependency component '%s'.", componentId), e);
				}
				
				try {
					method.invoke(object, new Object[] {component});
				} catch (Exception e) {
					throw new RuntimeException(String.format("Can't inject dependency '%s'.", componentId), e);
				}
			}
		}
		
	}
	
	private Object getComponent(String id, Class<?> intf) {
		Enhancer enhancer = new Enhancer();
		
        enhancer.setSuperclass(intf);
        enhancer.setCallback(new LazyLoadComponentInvocationHandler(id));
        enhancer.setUseFactory(false);
        enhancer.setClassLoader(new CompositeClassLoader(
        		new ClassLoader[] {intf.getClassLoader(), this.getClass().getClassLoader()}));
        
		return enhancer.create();
	}
	
	private class CompositeClassLoader extends ClassLoader {
		private ClassLoader[] classLoaders;
		
		public CompositeClassLoader(ClassLoader[] classLoaders) {
			this.classLoaders = classLoaders;
		}
		
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			for (ClassLoader classLoader : classLoaders) {
				try {
					Class<?> clazz = classLoader.loadClass(name);
					if (clazz != null)
						return clazz;
				} catch (ClassNotFoundException e) {
					// ignore
				}
			}
			
			throw new ClassNotFoundException(name);
		}
	}
	
	private class LazyLoadComponentInvocationHandler implements InvocationHandler {
		private String id;
		private volatile Object component = null;
		
		public LazyLoadComponentInvocationHandler(String id) {
			this.id = id;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			component = getComponent();
			
			if (component == null)
				throw new IllegalStateException(String.format("Null dependency '%s'.", id));
			
			return method.invoke(component, args);
		}

		private Object getComponent() throws CreationException {
			if (component != null)
				return component;
			
			synchronized (this) {
				if (component == null) {
					IComponentInfo componentInfo = components.get(id);
					if (componentInfo == null)
						return null;
					
					component = componentInfo.create();
				}
				
				return component;
			}
		}
		
	}
		
	private boolean isSetterMethod(Method method) {
		String methodName = method.getName();
		if (methodName.length() > 4 && methodName.startsWith("set") && Character.isUpperCase(methodName.charAt(3))) {
			Class<?>[] types = method.getParameterTypes();
			if (types.length != 1)
				return false;
			
			if (types[0].isPrimitive())
				return false;
			
			return true;
		}
		
		return false;
	}

	@Override
	public void found(ContributionClass<AppComponent> contributionClass) {
		BundleContext bundleContext = contributionClass.getBundleContext();
		Class<?> clazz = contributionClass.getType();
		AppComponent componentAnnotation = contributionClass.getAnnotation();
		if (componentAnnotation != null) {
			IComponentInfo componentInfo = new AppComponentInfo(componentAnnotation.value(), clazz,
					bundleContext, componentAnnotation.singleton());
			if (componentInfo instanceof IApplicationComponentServiceAware) {
				((IApplicationComponentServiceAware)componentInfo).setApplicationComponentService(this);
			}
			
			if (components.putIfAbsent(componentInfo.getId(), componentInfo) != null) {
				throw new RuntimeException(String.format("Application component id[%s] has already existed.",
						componentInfo.getId()));
			}
		}
	}

	@Override
	public void lost(ContributionClass<AppComponent> contributionClass) {
		String id = contributionClass.getAnnotation().value();
		componentLost(id);
	}
	
	private class AppComponentInfo implements IComponentInfo {
		private String id;
		private Class<?> type;
		private BundleContext bundleContext;
		private boolean singleton;
		private volatile Object instance;
		private Object singletonLock = new Object();
		
		public AppComponentInfo(String id, Class<?> type, BundleContext bundleContext, boolean singleton) {
			this.id = id;
			this.type = type;
			this.bundleContext = bundleContext;
			this.singleton = singleton;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public void addDependency(IDependencyInfo dependency) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeDependency(IDependencyInfo dependency) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IDependencyInfo[] getDependencies() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isAvailable() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isService() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object create() throws CreationException {
			if (!singleton) {
				try {
					return doCreate();
				} catch (Exception e) {
					throw new CreationException(String.format("Can't create application component %s.", id), e);
				}
			}
			
			synchronized (singletonLock) {
				if (instance == null) {
					try {
						instance = doCreate();
					} catch (Exception e) {
						throw new CreationException(String.format("Can't create application component %s.", id), e);
					}
				}
				
				return instance;
			}
		}

		private Object doCreate() throws InstantiationException, IllegalAccessException {
			Object component = type.newInstance();
			inject(component, bundleContext);
			
			return component;
		}
		
		@Override
		public boolean isSingleton() {
			return singleton;
		}

		@Override
		public BundleContext getBundleContext() {
			return bundleContext;
		}

		@Override
		public IComponentInfo getAliasComponent(String alias) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String toString() {
			return String.format("Application component[%s, %s, %s].", id, type, bundleContext);
		}
	}

}
