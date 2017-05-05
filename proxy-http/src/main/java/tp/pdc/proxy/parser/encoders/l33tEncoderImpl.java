package tp.pdc.proxy.parser.encoders;

import tp.pdc.proxy.parser.interfaces.l33tEncoder;

public class l33tEncoderImpl implements l33tEncoder {

    public byte encodeByte(byte c) {
        switch (c) {
            case 'a':
                return '4';

            case 'e':
                return '3';

            case 'i':
                return '1';

            case 'o':
                return '0';

            case 'c':
                return '<';

            default:
                return c;
        }
    }

    public byte decodeByte(byte c) {
        switch(c){
            case '4':
                return 'a';

            case '3':
                return 'e';

            case '1':
                return 'i';

            case '0':
                return 'o';

            case '<':
                return 'c';

            default:
                return c;
        }
    }
}

