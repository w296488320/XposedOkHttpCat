package zhenxi.xposedinto;

/**
 * Created by Lyh on
 * 2019/8/16
 */
import com.github.megatronking.stringfog.Base64;
import com.github.megatronking.stringfog.IStringFog;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * StringFog AES encrypt and decrypt implementation.
 *
 * @author Megatron King
 * @since 2018/9/2 14:38
 */
public final class StringFogImpl implements IStringFog {

    private static final int KEY_LENGTH = 16;

    private static final String AES_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final String IV = "1234123412341234";

    private static final String CHARSET_NAME_UTF_8 = "UTF-8";

    private String mEncryptedKey;
    private Cipher mEncryptedCipher;

    private String mDecryptedKey;
    private Cipher mDecryptedCipher;

    @Override
    public String encrypt(String data, String key) {
        if (key == null) {
            throw new IllegalArgumentException("Invalid AES key length: " + 0 + " bytes");
        }
        if (key.length() != KEY_LENGTH) {
            throw new IllegalArgumentException("Invalid AES key length: " + key.length() + " bytes");
        }
        initAESEncryptCipherIfNecessary(key);
        if (mEncryptedCipher == null) {
            // Init cipher failed, do nothing.
            return data;
        }
        try {
            byte[] raw = data.getBytes(CHARSET_NAME_UTF_8);
            return new String(Base64.encode(mEncryptedCipher.doFinal(raw), Base64.NO_WRAP), CHARSET_NAME_UTF_8);
        } catch (UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException e) {
            // Encrypt failed.
            return data;
        }
    }

    @Override
    public String decrypt(String data, String key) {
        if (key == null) {
            throw new IllegalArgumentException("Invalid AES key length: " + 0 + " bytes");
        }
        if (key.length() != KEY_LENGTH) {
            throw new IllegalArgumentException("Invalid AES key length: " + key.length() + " bytes");
        }
        initAESDecryptCipherIfNecessary(key);
        if (mDecryptedCipher == null) {
            // Init cipher failed, do nothing.
            return data;
        }
        try {
            byte[] raw = data.getBytes(CHARSET_NAME_UTF_8);
            return new String(mDecryptedCipher.doFinal(Base64.decode(raw, Base64.NO_WRAP)), CHARSET_NAME_UTF_8);
        } catch (UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException e) {
            // Decrypt failed.
            return data;
        }
    }

    @Override
    public boolean overflow(String data, String key) {
        // CBC has some padding length, I think 65535/2 is absolute ok.
        return data.length() * 4 / 3 >= 65535 / 2;
    }

    private void initAESEncryptCipherIfNecessary(String key) {
        if (mEncryptedKey == null || !mEncryptedKey.equals(key)) {
            try {
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(CHARSET_NAME_UTF_8), AES_ALGORITHM);
                IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes(CHARSET_NAME_UTF_8));
                mEncryptedCipher = Cipher.getInstance(TRANSFORMATION);
                mEncryptedCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                mEncryptedKey = key;
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | UnsupportedEncodingException
                    | InvalidKeyException | InvalidAlgorithmParameterException e) {
                mDecryptedCipher = null;
                mDecryptedKey = null;
            }
        }
    }

    private void initAESDecryptCipherIfNecessary(String key) {
        if (mDecryptedKey == null || !mDecryptedKey.equals(key)) {
            try {
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(CHARSET_NAME_UTF_8), AES_ALGORITHM);
                IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes(CHARSET_NAME_UTF_8));
                mDecryptedCipher = Cipher.getInstance(TRANSFORMATION);
                mDecryptedCipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                mDecryptedKey = key;
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | UnsupportedEncodingException
                    | InvalidKeyException | InvalidAlgorithmParameterException e) {
                mDecryptedCipher = null;
                mDecryptedKey = null;
            }
        }
    }

}
