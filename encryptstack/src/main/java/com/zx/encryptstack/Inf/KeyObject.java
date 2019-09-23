package com.zx.encryptstack.Inf;

import java.security.Key;

/**
 * Created by Lyh on
 * 2019/9/23
 */
public class KeyObject implements Key {
    @Override
    public String getAlgorithm() {
        return null;
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public byte[] getEncoded() {
        return new byte[0];
    }
}
