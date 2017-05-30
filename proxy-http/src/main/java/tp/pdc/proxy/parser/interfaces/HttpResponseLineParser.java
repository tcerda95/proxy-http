package tp.pdc.proxy.parser.interfaces;

public interface HttpResponseLineParser extends HttpVersionParser {
    int getStatusCode();
    boolean hasStatusCode();
}
