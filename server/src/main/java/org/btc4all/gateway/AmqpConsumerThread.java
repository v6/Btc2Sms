package org.btc4all.gateway;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.btc4all.gateway.pojo.EnvayaEvent;
import org.btc4all.gateway.pojo.EnvayaEvent.Event;
import org.btc4all.gateway.pojo.EnvayaMessage;
import org.btc4all.gateway.pojo.EnvayaRequest.Status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plivo.helper.api.client.RestAPI;
import com.plivo.helper.exception.PlivoException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class AmqpConsumerThread extends Thread {
    
    private boolean isActive = true;
    private String queueName;
    private ConnectionFactory factory;
    private ObjectMapper om;
    private EnvayaClient client;
    
    public AmqpConsumerThread(String queueName, ConnectionFactory factory, EnvayaClient client){
        isActive = true;
        this.queueName = queueName;
        this.factory = factory;
        this.om = new ObjectMapper();
        this.client = client;
    }
    
    @Override
    public void run() {
        while (isActive){
            Connection connection = null;
            QueueingConsumer consumer = null;
            try {
                connection = factory.newConnection();
                Channel channel = connection.createChannel();
                consumer = new QueueingConsumer(channel);
                channel.basicConsume(queueName, true, consumer);
            } catch (IOException e) {
                e.printStackTrace();
                isActive = false;
            }
            while (isActive&&connection.isOpen()) {
                try {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    EnvayaEvent e = om.readValue(delivery.getBody(), EnvayaEvent.class);
                    System.out.println("event received: " + om.writeValueAsString(e));
                    if (e.getEvent()==Event.SEND){
                        EnvayaMessage message = e.getMessages().get(0);
                        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
                        params.put("src", GatewayServletConfig.mobile);
                        params.put("dst", message.getTo());
                        params.put("text", message.getMessage());
    
                        try {
                            RestAPI restAPI = new RestAPI(GatewayServletConfig.plivoKey,
                                    GatewayServletConfig.plivoSecret, "v1");
                            System.out.println("plivo api set up.");
                            restAPI.sendMessage(params);
                            System.out.println("sent message to plivo successfully.");
                            client.sendStatus(Status.SENT, message.getId(), null);
                            System.out.println("envaya notified");
                        } catch (PlivoException | EnvayaClientException ex) {
                            ex.printStackTrace();
                        }
                    }else{
                        System.out.println("undigested message: "+om.writeValueAsString(e));
                    }
                } catch (ShutdownSignalException | ConsumerCancelledException| InterruptedException | IOException e) {
                    e.printStackTrace();
                    isActive = false;
                } finally{
                    if (null!=connection){
                        try {
                            connection.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            System.out.println("amqp connection closed");
        }
    }
    
    public void kill() {
        isActive = false;
        this.interrupt();
    }

}
