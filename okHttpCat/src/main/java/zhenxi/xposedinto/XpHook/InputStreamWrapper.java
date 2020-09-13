package zhenxi.xposedinto.XpHook;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamWrapper extends InputStream {

    private InputStream in;

    private ByteArrayOutputStream mirror;

    private int size;


    public InputStreamWrapper(InputStream in) throws IOException {
        mirror = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        int len;
        while ((len = in.read(buff)) != -1) {
            mirror.write(buff, 0, len);
            size += len;
        }
        mirror.flush();
        this.in = new ByteArrayInputStream(mirror.toByteArray());
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    public int size() {
        return size;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
        in.close();
        mirror.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public InputStream clone() {
        return new ByteArrayInputStream(mirror.toByteArray());
    }

}
