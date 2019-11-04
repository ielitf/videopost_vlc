package com.ceiv.communication;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class DMSDataDecoder extends CumulativeProtocolDecoder {

    private final static String TAG = "DMSDataDecoder";

    @Override
    protected boolean doDecode(IoSession ioSession, IoBuffer ioBuffer, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {
        //这里解码器只简单的对长度做判断，主要是起到一个粘包和断包的处理

        if (ioBuffer.remaining() <= 6) {
            return false;
        }
        ioBuffer.mark();
        byte[] tmp = new byte[2];
        ioBuffer.get(tmp);
        int msg_length = ((tmp[0] << 8) & 0xff00) | (tmp[1] & 0xff);

        if (ioBuffer.remaining() < msg_length) {
            ioBuffer.reset();
            return false;
        } else {
            ioBuffer.reset();
            tmp = new byte[2 + msg_length];
            ioBuffer.get(tmp, 0, tmp.length);
            protocolDecoderOutput.write(IoBuffer.wrap(tmp));
            if (ioBuffer.remaining() > 0) {
                return true;
            }
        }
        return false;
    }
}
