package tp.pdc.proxy.parser.interfaces;

public interface HttpResponseLineParser extends Parser, Reseteable {
    int getStatusCode();
    boolean hasStatusCode();
}
