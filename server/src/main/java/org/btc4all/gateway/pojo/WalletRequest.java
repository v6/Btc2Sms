package org.btc4all.gateway.pojo;

public class WalletRequest {
    
    private String xpub;
    private String accountId;
    
    public String getXpub() {
        return xpub;
    }
    public WalletRequest setXpub(String xpub) {
        this.xpub = xpub;
        return this;
    }
    public String getAccountId() {
        return accountId;
    }
    public WalletRequest setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

}
