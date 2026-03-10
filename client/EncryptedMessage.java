public class EncryptedMessage {

    public String wrappedKey;
    public String nonce;
    public String ciphertext;

    public EncryptedMessage(
            String wrappedKey,
            String nonce,
            String ciphertext
    ) {

        this.wrappedKey = wrappedKey;
        this.nonce = nonce;
        this.ciphertext = ciphertext;
    }
}