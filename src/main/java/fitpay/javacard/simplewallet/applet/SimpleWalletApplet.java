package fitpay.javacard.simplewallet.applet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class SimpleWalletApplet extends Applet {

    final static byte WALLET_CLA = (byte)0xb0;
    final static byte DEBIT = (byte)0x30;
    final static byte CREDIT = (byte)0x40;
    final static byte GET_BALANCE = (byte)0x50;
    final static byte TOGGLE_DEBUG = (byte)0x60;
    
    final static byte SW_INVALID_TRANSACTION_AMOUNT = (byte)0x6a83;
    final static byte SW_NEGATIVE_BALANCE = (byte)0x85;
    
    private short balance = 0;
    private boolean debug = false;
    
    private SimpleWalletApplet(byte[] bArray, short bOffset, byte bLength) {
        register();
    }
    
    public static void install(byte bArray[], short bOffset, byte bLength) throws ISOException {
        new SimpleWalletApplet(bArray, bOffset, bLength);
    }
    
    @Override
    public boolean select() {
        return true;
    }
    
    @Override
    public void process(APDU apdu) throws ISOException {
        byte[] buf = apdu.getBuffer();
                
        if ((buf[ISO7816.OFFSET_CLA] == 0) && (buf[ISO7816.OFFSET_INS] == (byte)0xa4)) {
            return;
        }
        
        if (buf[ISO7816.OFFSET_CLA] != WALLET_CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        
        switch (buf[ISO7816.OFFSET_INS]) {
        case GET_BALANCE: 
            getBalance(apdu);
            break;
        case DEBIT:
            debit(apdu);
            break;
        case CREDIT:
            credit(apdu);
            break;
        case TOGGLE_DEBUG:
            debug = !debug;
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void getBalance(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short le = apdu.setOutgoing();
        
        if (le < 2) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        apdu.setOutgoingLength((byte)2);
        buf[0] = (byte)(balance >> 8);
        buf[1] = (byte)(balance & 0xff);
        apdu.sendBytes((short)0, (short)2);
    }
    
    private void debit(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        byte read = (byte)apdu.setIncomingAndReceive();
        
        if (read != 1) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        byte amount = buf[ISO7816.OFFSET_CDATA];
        
        if ((short)(balance - amount) < 0) {
            ISOException.throwIt(SW_NEGATIVE_BALANCE);
        }
        
        balance = (short)(balance - amount);
    }
    
    private void credit(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        byte read = (byte)apdu.setIncomingAndReceive();
        
        if (read != 1) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        byte amount = buf[ISO7816.OFFSET_CDATA];
        
        if (amount < 0) {
            ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
        }
                
        balance = (short)(balance + amount);
    }    
}
