package org.btc4all.gateway;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Calendar;

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
import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.MemoryBlockStore;
import com.google.bitcoin.wallet.DeterministicKeyChain;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.bitcoin.wallet.KeyChain.KeyPurpose;
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
     * @throws BlockStoreException 
     */
    public static void main(String[] args) throws IOException, BlockStoreException {
        BlockChain chain = null;
        NetworkParameters params = TestNet3Params.get();
        //set up keychaingroup
        DeterministicSeed seed = new DeterministicSeed(SEED, MnemonicCode.BIP39_STANDARDISATION_TIME_SECS);
        KeyChainGroup kcg = new KeyChainGroup(seed);
        kcg.setLookaheadSize(LOOKAHEAD_SIZE);
        DeterministicKeyChain kc = kcg.getActiveKeyChain();
        String xpub1 = kc.getWatchingKey().serializePubB58();
        
        //get second half of wallet
        WalletClient wc = new WalletClient("http://127.0.0.1:8080" + WalletResource.PATH);
        WalletRequest wr = wc.create(xpub1);
        String xpub2 = wr.getXpub();
        System.out.println(kc.getWatchingKey().toString());
        kcg.addShadow(kc.getWatchingKey().getPath(), Arrays.asList(xpub2));
        
        //get wallet and peergroup going
        Wallet wallet=new Wallet(params, kcg);
        BlockStore blockStore = new MemoryBlockStore(params);
        chain = new BlockChain(params, wallet, blockStore);
        PeerGroup peerGroup = new PeerGroup(params, chain);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        long now = cal.getTimeInMillis() / 1000;
        peerGroup.setFastCatchupTimeSecs(now);
        peerGroup.setUserAgent("bip38 claimer", "0.1");
        peerGroup.addAddress(InetAddress.getLocalHost());
        peerGroup.addWallet(wallet);
        
        //create lookahead
        wallet.freshAddress(KeyPurpose.RECEIVE_FUNDS);
        
        //run and display balance
        wallet.addEventListener(new AbstractWalletEventListener() {
            @Override
            public synchronized void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("\nReceived tx " + tx.getHashAsString());
                System.out.println(tx.toString());
            }
        });
        peerGroup.startAsync();
        peerGroup.downloadBlockChain();
        System.out.println("available balance: "+wallet.getBalance());
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
