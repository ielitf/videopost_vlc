package com.ceiv.communication;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class DMSCodecFactory implements ProtocolCodecFactory {
    private DMSDataDecoder decoder;
    private DMSDataEncoder encoder;

    public DMSCodecFactory() {
        encoder = new DMSDataEncoder();
        decoder = new DMSDataDecoder();
    }


    @Override
    public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception {
        return decoder;
    }
}
