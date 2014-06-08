package org.btc4all.gateway;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.btc4all.gateway.pojo.WalletRequest;
import org.btc4all.gateway.resources.WalletResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.wallet.DeterministicKeyChain;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.bitcoin.wallet.KeyChainGroup;

public class WalletClient {
    private static byte[] SEED = Sha256Hash.create("never ever use a string seed like this for your wallet".getBytes()).getBytes();
    private static int LOOKAHEAD_SIZE = 10;
    private HttpClient httpClient;
    private String url;
    
    public static boolean isSucceed(HttpResponse response) {
        return response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300;
    }
    
    public static HttpClientBuilder getClientBuilder() {
        return HttpClientBuilder.create()
                .setRedirectStrategy(new DefaultRedirectStrategy() {
                    @Override
                    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
                        return response.getStatusLine().getStatusCode() == 308 || super.isRedirected(request, response, context);
                    }
                });
    }
    
    public WalletClient(String url) {
        httpClient = getClientBuilder().build();
        this.url = url;
    }

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        
        DeterministicSeed seed = new DeterministicSeed(SEED, MnemonicCode.BIP39_STANDARDISATION_TIME_SECS);
        KeyChainGroup kcg = new KeyChainGroup(seed);
        kcg.setLookaheadSize(LOOKAHEAD_SIZE);
        DeterministicKeyChain kc = kcg.getActiveKeyChain();
        String xpub = kc.getWatchingKey().serializePubB58();
        
        
        WalletClient wc = new WalletClient("http://127.0.0.1:8037" + WalletResource.PATH);
        WalletRequest wr = wc.create(xpub);
        System.out.println(wr.getAccountId());
        System.out.println(wr.getXpub());
        
        System.out.println(wc.get(wr.getAccountId()).getXpub());
    }
    
    public WalletRequest get(String account) throws IOException{
        HttpGet req = new HttpGet(url+"/account/"+account);
        return getPayload(req, WalletRequest.class);
    }
    
    public WalletRequest create(String xpub) throws IOException{
        HttpPost req = new HttpPost(url);
        String reqValue = null;
        try {
            reqValue = new ObjectMapper().writeValueAsString(new WalletRequest().setXpub(xpub));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        StringEntity entity = new StringEntity(reqValue, "UTF-8");
        entity.setContentType("application/json");
        req.setEntity(entity);
        return getPayload(req, WalletRequest.class);
    }
    
    protected <K> K getPayload(HttpRequestBase request, Class<K> entityClass) throws IOException {
        HttpResponse response;
        try {
            response = httpClient.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (isSucceed(response) && request.getMethod()!="DELETE"){
            return parsePayload(response, entityClass);
        }else if (isSucceed(response)){
            return null;
        }else{
            throw new IOException(response.getStatusLine().getReasonPhrase());
        }
    }
    
    protected <K> K parsePayload(HttpResponse response, Class<K> entityClass) throws IOException {
        return new ObjectMapper().readValue(response.getEntity().getContent(), entityClass);
    }

}
