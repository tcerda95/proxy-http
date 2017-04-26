package tp.pdc.proxy.parser.interfaces;

public interface HttpResponseLineParser extends Parser {
    public int getStatusCode();
    public boolean hasStatusCode();
}
