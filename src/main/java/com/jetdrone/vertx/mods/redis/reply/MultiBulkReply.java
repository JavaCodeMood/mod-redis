package com.jetdrone.vertx.mods.redis.reply;

import com.jetdrone.vertx.mods.redis.RedisDecoder;
import org.jboss.netty.buffer.ChannelBuffer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static com.jetdrone.vertx.mods.redis.util.Encoding.NEG_ONE_WITH_CRLF;
import static com.jetdrone.vertx.mods.redis.util.Encoding.numToBytes;

/**
 * Nested replies.
 */
public class MultiBulkReply implements Reply<Reply[]> {
    public static final char MARKER = '*';
    private final Reply[] replies;
    private final int size;
    private int index = 0;

    public MultiBulkReply(RedisDecoder rd, ChannelBuffer is) throws IOException {
        long l = RedisDecoder.readLong(is);
        if (l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
        }
        size = (int) l;
        if (size == -1) {
            replies = null;
        } else {
            if (size < 0) {
                throw new IllegalArgumentException("Invalid size: " + size);
            }
            replies = new Reply[size];
            read(rd, is);
        }
    }

    public void read(RedisDecoder rd, ChannelBuffer is) throws IOException {
        for (int i = index; i < size; i++) {
            replies[i] = rd.receive(is);
            index = i + 1;
            rd.checkpoint();
        }
    }

    @Override
    public Reply[] data() {
        return replies;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.MultiBulk;
    }
}