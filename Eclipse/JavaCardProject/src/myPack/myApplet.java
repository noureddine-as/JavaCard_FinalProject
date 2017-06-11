package myPack;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;

public class myApplet extends Applet {

    final static byte[] myFILE_SFI = {(byte) 0xA0, (byte) 0xA0};
    private byte[] myFile_content = new byte[256];
    private boolean myfile_selected = false;

    private final static byte myCLA = (byte) 0x80;

    private final static byte INS_SELECT_FILE = (byte) 0xA4;
    private final static byte INS_WRITE_FILE = (byte) 0xB0;
    private final static byte INS_READ_FILE = (byte) 0xD0;
    private final static byte INS_SUBMIT_PIN = (byte) 0xAA;
    private final static byte INS_CHANGE_PIN = (byte) 0xBB;
    private final static byte INS_UNBLOCK_PIN = (byte) 0xFF;
    private final static byte INS_SUBMIT_IC = (byte) 0xCC;  // submit IC code
    private final static byte INS_CHANGE_IC = (byte) 0xDD;  // submit IC code

    //------ PIN CODE ------
    private final static byte[] INIT_PIN = {(byte) 0x0A, (byte) 0x0B, (byte) 0x0C, (byte) 0x0D};
    private final static byte PIN_MAX_LENGTH = 0x08;
    private final static byte PIN_MAX_TRIES = 0x05;

    private OwnerPIN myPIN;

    //------ IC CODE ------
    private final static byte[] INIT_IC = {(byte) 0x0A, (byte) 0x0B, (byte) 0x0C, (byte) 0x0D};
    private final static byte IC_MAX_LENGTH = 0x08;
    private final static byte IC_MAX_TRIES = 0x05;

    private OwnerPIN myIC;

    public static void install(byte bArray[], short bOffset, byte bLength) throws ISOException {
        new myApplet();
    }

    public myApplet() {
        register();

        myPIN = new OwnerPIN(PIN_MAX_TRIES, PIN_MAX_LENGTH);
        myPIN.update(INIT_PIN, (byte) 0, (byte) 4);

        myIC = new OwnerPIN(IC_MAX_TRIES, IC_MAX_LENGTH);
        myIC.update(INIT_IC, (byte) 0, (byte) 4);

        myfile_selected = false;
    }

    public void process(APDU apdu) throws ISOException {
        if (selectingApplet()) {
            return;
        }

        byte[] buff = apdu.getBuffer();

        if (buff[ISO7816.OFFSET_CLA] != myCLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        switch (buff[ISO7816.OFFSET_INS]) {
            case INS_SELECT_FILE:
                // In this case the APDU command should be for example
                // CLA	INS	 P1		P2		length	file_sfi
                // 0x80 0xA4 0x00	0x00    0x02 	0xA0 0xA0 
                if (buff[ISO7816.OFFSET_CDATA] == myFILE_SFI[0] && buff[ISO7816.OFFSET_CDATA + 1] == myFILE_SFI[1]) {
                    myfile_selected = true;

                    /*	buff[0] = (byte)0x90;
            		buff[1] = (byte)0x00;
            		apdu.setOutgoingAndSend((byte)0, (byte)2); // -->> OK
                     */
                } else {
                    ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
                }
                break;

            case INS_WRITE_FILE:
                // In this case the APDU command should be
                // CLA  INS 	P1		P2		given_data_length	data
                // 0x80 0xB0	0x00 	0x00 	0x05 				0x01 0x02 ... 0x05    --->>> Writes 5 bytes {0x01 0x02 ... 0x05}
                // NOTE: the file musn't be reached if the file isn't (selected && pincode submit)

                if (myfile_selected) // and pinCOde submit
                {
                	if(myIC.isValidated())
                	{
                		byte len = buff[ISO7816.OFFSET_LC];

                        for (byte i = 0; i < len; i++) {
                            myFile_content[i] = buff[ISO7816.OFFSET_CDATA + i];
                        }

                        /*	buff[0] = (byte)(len); // The number of the bytes that have been written successfully
    					buff[1] = (byte)0x90;
                		buff[2] = (byte)0x00;
                		buff[ISO7816.SW]
                		apdu.setOutgoingAndSend((byte)0, (byte)3); // -->> OK
                         */
                        if (len == 255) {
                            ISOException.throwIt(ISO7816.SW_FILE_FULL);
                        }
                		
                	}else {
                        ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);            
                	}
                    

                } else {
                    ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
                }
                break;

            case INS_READ_FILE:
                // In this case the APDU command should be
                // CLA  INS 	reading_offset	length		
                // 0x80 0xD0	0x00 			0x05 		0x00     -->> Reads 5 bytes beginning from 0
                // NOTE: the file musn't be reached if the file isn't (selected && pincode submit)

                if (myfile_selected) // and pinCOde submit
                {
                	if(myPIN.isValidated())
                	{
                		byte offset = buff[ISO7816.OFFSET_P1];
                        byte len = buff[ISO7816.OFFSET_P2];

                        if (offset + len > 255) {
                            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                        }

                        for (byte i = 0; i < len; i++) {
                            buff[i] = myFile_content[offset + i];
                        }

                        // return data
                        apdu.setOutgoingAndSend((byte) 0, (byte) len); // -->> OK
                	}else{
                        ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                       }
                    
                } else {
                    ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
                }
                break;

            case INS_SUBMIT_PIN:
                // CLA	INS		P1		P2		Pin_length	Pin bytes	
                // 0x80 0xAA 	0x00 	0x00 	0x04		0x0A 0x0B .. rest of the PIN

                if (!myPIN.check(buff, ISO7816.OFFSET_CDATA, buff[ISO7816.OFFSET_LC])) {
                    buff[0] = myPIN.getTriesRemaining();
                    apdu.setOutgoingAndSend((byte) 0, (byte) 1);
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }

                break;
            case INS_SUBMIT_IC:
                // CLA	INS		P1		P2		IC_length	IC bytes	
                // 0x80 0xCC 	0x00 	0x00 	0x04		0x0A 0x0B .. rest of the PIN

                if (!myIC.check(buff, ISO7816.OFFSET_CDATA, buff[ISO7816.OFFSET_LC])) {
                    buff[0] = myIC.getTriesRemaining();
                    apdu.setOutgoingAndSend((byte) 0, (byte) 1);
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }

                break;
            case INS_CHANGE_PIN:
                // to reset PIN u need to have the IC
                // CLA	INS		P1		P2		Pin_length	Pin bytes	
                // 0x80 0xBB 	0x00 	0x00 	0x04		0x0A 0x0B .. rest of the PIN
                // if LC == 0 -->> Reset
            	
                if (myIC.isValidated()) {
                    myPIN.update(buff, ISO7816.OFFSET_CDATA, buff[ISO7816.OFFSET_LC]);
                    
                } else {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }

                break;
            case INS_CHANGE_IC:
                // CLA	INS		P1		P2		IC_length	IC bytes	
                // 0x80 0xDD 	0x00 	0x00 	0x04		0x0A 0x0B .. rest of the PIN
                // if LC == 0 -->> Reset

                if (myIC.isValidated()) {
                	myIC.update(buff, ISO7816.OFFSET_CDATA, buff[ISO7816.OFFSET_LC]);
                    
                } else {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }

                break;
            
            case INS_UNBLOCK_PIN:
            	if (myIC.isValidated()) {
                	myPIN.resetAndUnblock();
                } else {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
            	break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }

    }

}
