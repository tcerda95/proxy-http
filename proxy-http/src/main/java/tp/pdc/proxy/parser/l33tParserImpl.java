package tp.pdc.proxy.parser;

import tp.pdc.proxy.parser.interfaces.l33tParser;
import java.nio.ByteBuffer;

public class l33tParserImpl implements l33tParser {

    @Override
    public void decode(ByteBuffer inputBuffer, ByteBuffer outputBuffer) {

        while(inputBuffer.hasRemaining() && outputBuffer.hasRemaining()){
            byte c = inputBuffer.get();
            outputBuffer.put(decodeByte(c));
        }
    }

    @Override
    public void encode(ByteBuffer inputBuffer, ByteBuffer outputBuffer) {
        while(inputBuffer.hasRemaining() && outputBuffer.hasRemaining()){
            byte c = inputBuffer.get();
            outputBuffer.put(encodeByte(c));
        }
    }

    private byte encodeByte(byte c) {
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

    private byte decodeByte(byte c) {
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

