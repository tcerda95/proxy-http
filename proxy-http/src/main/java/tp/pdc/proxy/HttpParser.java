package tp.pdc.proxy;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public class HttpParser {

    private static final char CR = (char) 13;
    private static final char LF = (char) 10;
    private static final char SP = (char) 32;

    private enum ParserState {
        /* First Line */
        REQUEST_START, METHOD_READ, URI_READ, HTTP_VERSION, CR_END_LINE, LF_END_LINE,

        READ_HEADERS,
        /* Error states */
        BAD_REQUEST_ERROR,
    }

    private enum HttpVersionState {
        NOT_READ_YET, H, T_1, T_2, P, MAJOR_VERSION, MINOR_VERSION, READ_OK, ERROR,
    }

    private static class States {
        ParserState parser;
        HttpVersionState versionParser;
        int httpMajorVersion;
        int httpMinorVersion;
    }

    private States states;

    private final CharBuffer methodName;
    private final CharBuffer httpURI;

    public HttpParser () {
        states = new States();
        states.parser = ParserState.REQUEST_START;
        states.versionParser = HttpVersionState.NOT_READ_YET;
        methodName = CharBuffer.allocate(16);
        httpURI = CharBuffer.allocate(256);
    }

    // Receives buffer in read state
    public void parse(final ByteBuffer readBuffer) {

        while (readBuffer.hasRemaining()) {
            char c = (char) readBuffer.get();

            switch (states.parser) {
                case REQUEST_START:
                    if (isALPHA(c)) {
                        states.parser = ParserState.METHOD_READ;
                        methodName.append(c);
                    } else {
                        states.parser = ParserState.BAD_REQUEST_ERROR;
                    }
                    break;

                case METHOD_READ:
                    if (isALPHA(c)) {
                        methodName.put(c);
                    } else if (c == SP) {
                        states.parser = methodNameIsOk() ? ParserState.URI_READ : ParserState.BAD_REQUEST_ERROR;
                    } else {
                        states.parser = ParserState.BAD_REQUEST_ERROR;
                    }
                    break;

                case URI_READ:
                    if (c == SP) {
                        states.parser = URIIsOk() ? ParserState.HTTP_VERSION : ParserState.BAD_REQUEST_ERROR;
                        httpURI.put(c);
                    } else if (isURIComponent(c)) {
                        httpURI.put(c);
                    } else {
                        states.parser = ParserState.BAD_REQUEST_ERROR;
                    }
                    break;

                case HTTP_VERSION:
                    parseHttpVersion(c);
                    break;

//                case CR_END_LINE:
//                    states.parser = ( c == CR ? ParserState.LF_END_LINE : ParserState.BAD_REQUEST_ERROR);
//                    break;

                case LF_END_LINE:
                    states.parser = ( c == LF ? ParserState.READ_HEADERS : ParserState.BAD_REQUEST_ERROR);
                    break;

                case READ_HEADERS:
                    System.out.println("READ JUST FINE!");
                    break;

                default:
                    states.parser = ParserState.BAD_REQUEST_ERROR;
                    return;
                    // TODO: k c yo
            }
        }
    }

    private void parseHttpVersion(char c) {
        HttpVersionState s = states.versionParser;
        switch (s) {
            case NOT_READ_YET:
                if (c == 'H')
                    states.versionParser = HttpVersionState.H;
                else
                    states.versionParser = HttpVersionState.ERROR;
                break;

            case H:
                if (c == 'T')
                    states.versionParser = HttpVersionState.T_1;
                else
                    states.versionParser = HttpVersionState.ERROR;
                break;

            case T_1:
                if (c == 'T')
                    states.versionParser = HttpVersionState.T_2;
                else
                    states.versionParser = HttpVersionState.ERROR;
                break;

            case T_2:
                if (c == 'P')
                    states.versionParser = HttpVersionState.P;
                else
                    states.versionParser = HttpVersionState.ERROR;
                break;

            case P:
                if (c == '/')
                    states.versionParser = HttpVersionState.MAJOR_VERSION;
                else
                    states.versionParser = HttpVersionState.ERROR;
                break;

            case MAJOR_VERSION:
                if (isNum(c) && c != '0') {
                    states.httpMajorVersion *= 10;
                    states.httpMajorVersion += c - '0';
                } else if (c == '.' && states.httpMajorVersion != 0) {
                    states.versionParser = HttpVersionState.MINOR_VERSION;
                } else {
                    states.versionParser = HttpVersionState.ERROR;
                }
                break;

            case MINOR_VERSION:
                if (isNum(c)) {
                    states.httpMinorVersion *= 10;
                    states.httpMinorVersion += c - '0';
                } else if (c == CR) {
                    states.versionParser = HttpVersionState.READ_OK;
                    states.parser = ParserState.LF_END_LINE;
                } else {
                    states.versionParser = HttpVersionState.ERROR;
                }
                break;

            case READ_OK:
                states.versionParser = HttpVersionState.ERROR;
                break;
        }
    }

    public boolean hasError() {
        return states.parser == ParserState.BAD_REQUEST_ERROR;
    }

    // TODO: Hacer bien estas cosas.
    private boolean methodNameIsOk() {
        methodName.flip();
        System.out.println("METHOD: " + methodName.toString());
        return methodName.toString().equals("GET") || methodName.equals("POST") || methodName.equals("HEAD");
    }

    private boolean URIIsOk() {
        httpURI.flip();
        System.out.println("URI: " + httpURI);
        return httpURI.toString().equals("/");
    }

    private boolean isURIComponent(final char c) {
        return c == '/';
    }

    private boolean isALPHA(final char c) {
        return c == 'G' || c == 'E' || c == 'T';
    }

    private boolean isNum(final char c) {
        return '0' <= c && c <= '9';
    }
    
    public static void main (String[] args) throws Exception {
        char cr = 13, lf = 10;
        String s = "GET / HTTP/1.1" + cr + lf + "ABC"; //CRLF

        for (int i = 0; i < s.length(); i++) {
            System.out.println("ITERACION: " + i);

            String s1 = s.substring(0, i);
            String s2 = s.substring(i);

            HttpParser parser = new HttpParser();
            ByteBuffer buf1 = ByteBuffer.wrap(s1.getBytes(StandardCharsets.US_ASCII));
            ByteBuffer buf2 = ByteBuffer.wrap(s2.getBytes(StandardCharsets.US_ASCII));

            parser.parse(buf1);
            parser.parse(buf2);
        }

    }
}
