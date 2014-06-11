package org.btc4all.gateway.resources;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.btc4all.gateway.pojo.EnvayaEvent;
import org.btc4all.gateway.pojo.EnvayaRequest;
import org.btc4all.gateway.pojo.EnvayaRequest.MessageType;
import org.btc4all.gateway.pojo.EnvayaRequest.Status;
import org.btc4all.gateway.pojo.EnvayaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * local service, service, exposed to gateways
 *
 * @author johann
 *
 */

@Path(EnvayaSmsResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class EnvayaSmsResource {
    public final static String PATH = "/envayasms";
    public static Logger log = LoggerFactory.getLogger(EnvayaSmsResource.class);
    
    private final EnvayaRequest req;
    
    @Inject public EnvayaSmsResource(ServletRequest request) {
        HttpServletRequest httpReq = (HttpServletRequest)request;
        req = (EnvayaRequest)httpReq.getAttribute("er");
    }
    
    @POST
    @Path("/{cn}/sms/")
    public EnvayaResponse receive(@PathParam("cn") String cn,
            @Context UriInfo uriInfo){
        try{
            switch (req.getAction()) {
                case SEND_STATUS:
                    MDC.put("hostName", cn);
                    MDC.put("mobile", req.getPhoneNumber());
                    MDC.put("event", req.getAction().toString());
                    MDC.put("log", req.getLog());
                    MDC.put("msgId", req.getId());
                    MDC.put("status", req.getStatus().toString());
                    MDC.put("error", req.getError());
                    log.info("send status received");
                    MDC.clear();
                    if (req.getStatus() == Status.SENT&&!req.getId().contains("SmsResource")){
                        //TODO: report successfull send
                    }
                    break;
                case TEST:
                    MDC.put("hostName", cn);
                    MDC.put("mobile", req.getPhoneNumber());
                    MDC.put("event", req.getAction().toString());
                    MDC.put("log", req.getLog());
                    log.info("test received");
                    MDC.clear();
                    break;
                case AMQP_STARTED:
                    MDC.put("hostName", cn);
                    MDC.put("mobile", req.getPhoneNumber());
                    MDC.put("event", req.getAction().toString());
                    MDC.put("log", req.getLog());
                    MDC.put("consumer_tag", req.getConsumerTag());
                    log.info("amqp started received");
                    MDC.clear();
                    break;
                case DEVICE_STATUS:
                    MDC.put("hostName", cn);
                    MDC.put("mobile", req.getPhoneNumber());
                    MDC.put("event", req.getAction().toString());
                    MDC.put("log", req.getLog());
                    MDC.put("status", req.getStatus().toString());
                    log.info("device status received");
                    MDC.clear();
                    break;
                case FORWARD_SEND:
                    MDC.put("hostName", cn);
                    MDC.put("mobile", req.getPhoneNumber());
                    MDC.put("event", req.getAction().toString());
                    MDC.put("log", req.getLog());
                    log.info("forward message "+ req.getMessage()+" send to "+ req.getTo()+" via "+ req.getMessageType() + " at " + req.getTimestamp());
                    MDC.clear();
                    break;
                case INCOMING:
                    if (req.getMessageType() == MessageType.SMS) {
                        MDC.put("hostName", cn);
                        MDC.put("mobile", req.getPhoneNumber());
                        MDC.put("event", req.getAction().toString());
                        MDC.put("log", req.getLog());
                        MDC.put("message_type", req.getMessageType().toString());
                        log.info("incoming message "+req.getMessage()+" received from "+req.getFrom()+" via "+req.getMessageType()+" at "+req.getTimestamp());
                        MDC.clear();
                        //TODO: Parse Message
                        
                    }
                break;
            }
        }catch(Exception e){
            log.warn("envaya call failed", e);
            e.printStackTrace();
        }
        return new EnvayaResponse().setEvents(new ArrayList<EnvayaEvent>());
    }

}
