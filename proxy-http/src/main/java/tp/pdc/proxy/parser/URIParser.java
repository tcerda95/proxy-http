package tp.pdc.proxy.parser;


import java.nio.ByteBuffer;

public class URIParser {

    private boolean gotHost;

    private ByteBuffer uriRead;
    private URIState state;

    private enum URIState {
        START, URI_WITH_HOST, URI_NO_HOST, URI_HOST_SLASH_1, URI_HOST_SLASH_2
    }

    public URIParser () {
        uriRead = ByteBuffer.allocate(64);
    }

    public void parse(ByteBuffer input, ByteBuffer output) {

    }
}
