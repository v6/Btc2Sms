package org.btc4all.gateway.resources;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.btc4all.gateway.EnvayaClient;
import org.btc4all.gateway.EnvayaClientException;
import org.btc4all.gateway.pojo.EnvayaRequest.MessageType;
import org.btc4all.gateway.pojo.EnvayaResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path(PlivoResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class PlivoResource {
    public final static String PATH = "/plivo";
    
    private EnvayaClient client;

    @Inject
    public PlivoResource(EnvayaClient client) {
        this.client = client;
    }

    @POST
    public void receive(@FormParam("Text") String text,
            @FormParam("Type") String type, @FormParam("From") String from,
            @FormParam("To") String to,
            @FormParam("MessageUUID") String messageUuid) throws JsonProcessingException {
        try {
            EnvayaResponse er = client.incoming("+"+from, MessageType.SMS, text, System.currentTimeMillis());
            System.out.println("send response: "+new ObjectMapper().writeValueAsString(er));
        } catch (EnvayaClientException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

    }

}
