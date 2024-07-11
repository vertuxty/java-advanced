package info.kgeorgiy.ja.leshchev.walk;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JenkinsHash extends FilterInputStream {

    private final int BUFF_SIZE = 2048;
    private final byte[] buffer;
    public JenkinsHash(InputStream inputStream) {
        super(inputStream);
        this.buffer = new byte[BUFF_SIZE];
    }


    @Override
    public int read() throws IOException {
        int hash = 0;
        int pointer = 0;
        while ((pointer = in.read(buffer)) != -1) {
            int i = 0;
            while (i != pointer) {
                hash += buffer[i] & 0xff;
                hash += hash << 10;
                hash ^= hash >>> 6;
                i++;
            }
        }
        hash += hash << 3;
        hash ^= hash >>> 11;
        hash += hash << 15;
        return hash;
    }
}
