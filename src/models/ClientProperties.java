package models;

import javax.crypto.SecretKey;

public class ClientProperties {
    private byte[] tag = null;
    private byte[] idx = null;
    private SecretKey secretKey = null;

    public ClientProperties(byte[] tag, byte[] idx, SecretKey secretKey) {
        this.tag = tag;
        this.idx = idx;
        this.secretKey = secretKey;
    }

    public byte[] getTag() {
        return tag;
    }

    public void setTag(byte[] tag) {
        this.tag = tag;
    }

    public byte[] getIdx() {
        return idx;
    }

    public void setIdx(byte[] idx) {
        this.idx = idx;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }
}
