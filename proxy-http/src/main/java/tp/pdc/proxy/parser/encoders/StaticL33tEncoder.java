package tp.pdc.proxy.parser.encoders;

import tp.pdc.proxy.parser.interfaces.l33tEncoder;

public class StaticL33tEncoder implements l33tEncoder {
    private static byte[] encode;
    private static byte[] decode;

    static {
        encode = new byte[256]; decode = new byte[256];
        for (int c = 0; c < 256; c++) {
            switch (c) {
                case 'a':
                    encode[c] = '4';
                    break;
                case 'e':
                    encode[c] = '3';
                    break;
                case 'i':
                    encode[c] = '1';
                    break;
                case 'o':
                    encode[c] = '0';
                    break;
                case 'c':
                    encode[c] = '<';
                    break;
                default:
                    encode[c] = (byte) c;
            }

            switch(c) {
                case '4':
                    decode[c] = 'a';
                    break;
                case '3':
                    decode[c] = 'e';
                    break;
                case '1':
                    decode[c] = 'i';
                    break;
                case '0':
                    decode[c] = 'o';
                    break;
                case '<':
                    decode[c] = 'c';
                    break;
                default:
                    decode[c] = (byte) c;
            }
        }
    }

    public static byte encodeByte(byte c) {
        return encode[c];
    }

    public static byte decodeByte(byte c) {
        return decode[c];
    }
}

