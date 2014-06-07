package org.btc4all.gateway.resources;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path(PlivoResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class PlivoResource {
    public final static String PATH = "/plivo";
    private HttpServletRequest httpReq;
    
    @Inject public PlivoResource(ServletRequest request){
        httpReq = (HttpServletRequest)request;
    }
    
    @POST
    public void receive(
            @FormParam("Text") String text,
            @FormParam("Type") String type,
            @FormParam("From") String from,
            @FormParam("To") String to,
            @FormParam("MessageUUID") String messageUuid){
        System.out.println(from);
        System.out.println(messageUuid);
        System.out.println(text);
        System.out.println(to);
        System.out.println(type);
    }
    
    @GET
    public void get(){
        
    }

}
