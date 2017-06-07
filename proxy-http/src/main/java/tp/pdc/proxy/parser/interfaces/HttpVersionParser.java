package tp.pdc.proxy.parser.interfaces;

public interface HttpVersionParser extends Parser, Reseteable {

    boolean readMinorVersion();

    boolean readMajorVersion();

    int getMajorHttpVersion();

    int getMinorHttpVersion();

    byte[] getWholeVersionBytes();
}
