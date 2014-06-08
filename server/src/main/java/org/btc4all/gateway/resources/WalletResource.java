package org.btc4all.gateway.resources;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.Protos.Key;
import org.btc4all.gateway.pojo.WalletRequest;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.bitcoin.wallet.KeyChainGroup;
import com.google.common.collect.ImmutableList;


@Path(WalletResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class WalletResource {
    public final static String PATH = "/wallet";
    private static byte[] SEED = Sha256Hash.create("don't use a string seed like this in real life".getBytes()).getBytes();
    private static int LOOKAHEAD_SIZE = 1;
    private static String STORAGE_KEY = "cainBackup";
    private Cache cache;
    
    @Inject 
    public WalletResource(Cache cache){
        this.cache = cache;
    }
    
    /**
     * creates a new account on the wallet
     */
    @POST
    public WalletRequest create(WalletRequest rw) throws UnreadableWalletException{
        KeyChainGroup kcg = getKeyChain();
        //find unused account
        boolean keyExists = true;
        int i = 0;
        do {
            try{
                kcg.getActiveKeyChain().getKey(ImmutableList.of(new ChildNumber(i, true)));
                i++;
            }catch(IllegalArgumentException e){
                keyExists = false;
            }
        } while (keyExists);
        
        //create key and return
        DeterministicKey account = kcg.getActiveKeyChain().getKey(ImmutableList.of(new ChildNumber(i, true)), true);
        kcg.addShadow(account.getPath(), Arrays.asList(rw.getXpub()));
        
        //persist changes
        List<Key> keys = kcg.serializeToProtobuf();
        cache.put(new Element(STORAGE_KEY,keys));
        return new WalletRequest().setXpub(account.serializePubB58()).setAccountId(Integer.toString(i));
    }
    
    /**
     * reads an account from the wallet and returns xpub
     * @throws UnreadableWalletException 
     */
    @GET
    @Path("/account/{account}")
    public WalletRequest get(@PathParam("account") String account) throws UnreadableWalletException{
        KeyChainGroup kcg = getKeyChain();
        int a = Integer.parseInt(account);
        ImmutableList<ChildNumber> path = ImmutableList.of(new ChildNumber(a, true));
        DeterministicKey key = null;
        try{
            key = kcg.getActiveKeyChain().getKey(path);
        }catch(IllegalArgumentException e){
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        }
        
        return new WalletRequest().setXpub(key.serializePubB58());
    }
    
    @SuppressWarnings("unchecked")
    private KeyChainGroup getKeyChain() throws UnreadableWalletException{
        Element e = cache.get(STORAGE_KEY);
        KeyChainGroup kcg = null;
        if (e!=null){
            //get keychaingroup from storage
            List<Protos.Key> keys = (List<Protos.Key>)e.getObjectValue();
            kcg = KeyChainGroup.fromProtobufUnencrypted(keys);
        }else{
            //create chain
            DeterministicSeed seed = new DeterministicSeed(SEED, MnemonicCode.BIP39_STANDARDISATION_TIME_SECS);
            kcg = new KeyChainGroup(seed);
            kcg.setLookaheadSize(LOOKAHEAD_SIZE);
            kcg.getActiveKeyChain();
            //persist
            cache.put(new Element(STORAGE_KEY,kcg.serializeToProtobuf()));
        }
        return kcg;
    }

}
