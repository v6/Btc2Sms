package org.btc4all.gateway;


import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class GatewayServletConfig extends GuiceServletContextListener {
    public static String mobile;
    public static String restEndpoint;
	public static String restPassword;
	public static String amqpEndpoint;
	public static String amqpUser;
	public static String amqpPassword;
	public static String plivoToken;
	public static String plivoSecret;
	public static String basePath;
	public static Logger log = LoggerFactory.getLogger(GatewayServletConfig.class);
	public static Injector injector;
	private ServletContext servletContext;

	static {
		mobile = System.getProperty("mobile");
		restEndpoint = System.getProperty("restEndpoint");
		restPassword = System.getProperty("restPassword");
		amqpEndpoint = System.getProperty("amqpEndpoint");
		amqpUser = System.getProperty("amqpUser");
		amqpPassword = System.getProperty("amqpPassword");
		plivoToken = System.getProperty("plivoToken");
        plivoSecret = System.getProperty("plivoSecret");
        basePath = System.getProperty("basePath");
	}
    
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		servletContext = servletContextEvent.getServletContext();
		super.contextInitialized(servletContextEvent);
		final Injector i = getInjector();
		log.info("ServletContextListener started");
	}
	
    @Override
    protected Injector getInjector(){
        injector = Guice.createInjector(new ServletModule(){
            @Override
            protected void configureServlets(){
            	filter("/plivo*").through(PlivoAuthFilter.class);
        	}
            @Provides @Singleton @SuppressWarnings("unused")
            public Cache provideHourCache(){
                //Create a singleton CacheManager using defaults
                CacheManager manager = CacheManager.create();
                //Create a Cache specifying its configuration.
                Cache testCache = new Cache(new CacheConfiguration("hour", 1000)
                    .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
                    .eternal(false)
                    .timeToLiveSeconds(7200)
                    .timeToIdleSeconds(3600)
                    .diskExpiryThreadIntervalSeconds(0));
                manager.addCache(testCache);
                return testCache;
            }
            @Provides @Singleton @SuppressWarnings("unused")
            public PlivoAuthFilter providePlivoFilter(){
                return new PlivoAuthFilter(GatewayServletConfig.plivoSecret, GatewayServletConfig.basePath);
            }});
        return injector;
    }
	
    @Override
	public void contextDestroyed(ServletContextEvent sce) {
		super.contextDestroyed(sce);
		log.info("ServletContextListener destroyed");
	}

}
