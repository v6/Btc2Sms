package org.btc4all.gateway;


import java.io.IOException;

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
import com.rabbitmq.client.ConnectionFactory;

public class GatewayServletConfig extends GuiceServletContextListener {
    public static String mobile;
    public static String restEndpoint;
	public static String restPassword;
	public static String amqpHost;
	public static String envayaUser;
	public static String envayaPassword;
	public static String basePath;
	public static String gatewayPath;
	public static String envayaPath;
	public static String envayaToken;
	public static String plivoKey;
	public static String plivoSecret;
	public static Logger log = LoggerFactory.getLogger(GatewayServletConfig.class);
	public static Injector injector;
	private AmqpConsumerThread act;
	private ConnectionFactory factory;

	static {
		mobile = System.getProperty("mobile");
		restEndpoint = System.getProperty("restEndpoint");
		restPassword = System.getProperty("restPassword");
		amqpHost = System.getProperty("amqpHost");
		envayaUser = System.getProperty("envayaUser");
		envayaPassword = System.getProperty("envayaPassword");
		plivoKey = System.getProperty("plivoKey");
        plivoSecret = System.getProperty("plivoSecret");
        envayaPath = System.getProperty("envayaPath");
        basePath = System.getProperty("basePath");
        gatewayPath = System.getProperty("gatewayPath");
        envayaToken = System.getProperty("envayaToken");
	}
    
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		super.contextInitialized(servletContextEvent);
		final Injector i = getInjector();
		EnvayaClient client = i.getInstance(EnvayaClient.class);
		factory = i.getInstance(ConnectionFactory.class);
		act = new AmqpConsumerThread(GatewayServletConfig.envayaUser, factory, client);
		act.start();
		log.info("ServletContextListener started");
	}
	
    @Override
    protected Injector getInjector(){
        injector = Guice.createInjector(new ServletModule(){
            @Override
            protected void configureServlets(){
            	filter("/plivo*").through(PlivoAuthFilter.class);
            	filter("/envayasms*").through(EnvayaFilter.class);
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
            public EnvayaFilter provideEnvayaFilter(){
                return new EnvayaFilter(GatewayServletConfig.envayaPassword, GatewayServletConfig.basePath);
            }
            @Provides @Singleton @SuppressWarnings("unused")
            public ConnectionFactory provideAmqpChannel() throws IOException{
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(GatewayServletConfig.amqpHost);
                return factory;
            }
            @Provides @Singleton @SuppressWarnings("unused")
            public EnvayaClient provideEnvayaClient(){
                return new EnvayaClient(GatewayServletConfig.envayaPath, GatewayServletConfig.envayaToken, GatewayServletConfig.mobile);
            }
            @Provides @Singleton @SuppressWarnings("unused")
            public PlivoAuthFilter providePlivoFilter(){
                return new PlivoAuthFilter(GatewayServletConfig.plivoSecret, GatewayServletConfig.gatewayPath);
            }});
        return injector;
    }
	
    @Override
	public void contextDestroyed(ServletContextEvent sce) {
        if (null!=act){
            act.kill();
        }
		super.contextDestroyed(sce);
		log.info("ServletContextListener destroyed");
	}

}
