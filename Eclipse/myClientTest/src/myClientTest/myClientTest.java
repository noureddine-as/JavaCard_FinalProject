package myClientTest;

import java.io.*;
import java.net.Socket;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadT1Client;
import com.sun.javacard.apduio.CadTransportException;



public class myClientTest {
	
    final static byte[] myFILE_SFI = {(byte) 0xA0, (byte) 0xA0};
    private final static byte myCLA = (byte) 0x80;

    private final static byte INS_SELECT_FILE = (byte) 0xA4;
    private final static byte INS_WRITE_FILE = (byte) 0xB0;
    private final static byte INS_READ_FILE = (byte) 0xD0;
    private final static byte INS_SUBMIT_PIN = (byte) 0xAA;
    private final static byte INS_CHANGE_PIN = (byte) 0xBB;
    private final static byte INS_UNBLOCK_PIN = (byte) 0xFF;
    private final static byte INS_SUBMIT_IC = (byte) 0xCC;  // submit IC code
    private final static byte INS_CHANGE_IC = (byte) 0xDD;  // submit IC code

    private final static byte[] PASS = {(byte) 0x0A, (byte) 0x0B, (byte) 0x0C, (byte) 0x0D};

	private static CadT1Client cad = null;
	private static Socket sckCarte;
	
	public static void main(String[] args) throws IOException, CadTransportException {
		
		connectToApplet();
		
		selectApplet();
		
		submitIC(PASS);
		submitPIN(PASS);
		
		selectFile(myFILE_SFI);
		
		byte[] content = {'h', 'e', 'l', 'l', 'o', ' ', 'C', 'S', 'E', 'E'};
		writeContent(content);
		
		readContent((byte)0, (byte)10);
		
		deselectApplet();
		
		
		System.out.println("Tout s'est bien passé!");
	}
	
	public static void connectToApplet()
	{
		try{
			sckCarte = new Socket("localhost", 9025);
			sckCarte.setTcpNoDelay(true);
			BufferedInputStream input = new BufferedInputStream(sckCarte.getInputStream());
			BufferedOutputStream output = new BufferedOutputStream(sckCarte.getOutputStream());
			
			cad = new CadT1Client(input, output);
			
		}catch(Exception e){
			System.out.println("Erreur à la connexion "+ e);
			return;
		}
		
		/*    PowerUp   */
		try {
			cad.powerUp();
		} catch (Exception e) {
			System.out.println("Erreur au powerUp "+ e);
			return;
		}
	}
	
	public static void selectApplet() throws IOException, CadTransportException
	{
		Apdu apdu = new Apdu();
		apdu.command[Apdu.CLA] = (byte)0x00;
		apdu.command[Apdu.INS] = (byte)0xA4;
		apdu.command[Apdu.P1] = (byte)0x04;
		apdu.command[Apdu.P2] = (byte)0x00;

		byte[] aid = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x00, 0x00};
		
		apdu.setDataIn(aid);
		cad.exchangeApdu(apdu);
		
		if (apdu.getStatus() != 0x9000) {
			System.out.println("Erreur lors de la sélection de l'applet   -    Erreur: " + Integer.toHexString(apdu.getStatus()));
			System.exit(1);
		}
	}
	
	public static void deselectApplet(){
		try {
			cad.powerDown();
		} catch (Exception e) {
			System.out.println("Erreur lors de l'envoi de la commande Powerdown a la Javacard");
			return;
		}
	}
	
	static void submitIC(byte[] iccode) throws IOException, CadTransportException
	{
		Apdu apdu = new Apdu();
		apdu.command[Apdu.CLA] = myCLA;
		apdu.command[Apdu.INS] = INS_SUBMIT_IC;
		apdu.command[Apdu.P1] = (byte)0x00;
		apdu.command[Apdu.P2] = (byte)0x00;
		apdu.setDataIn(iccode);
		cad.exchangeApdu(apdu);
		
		if(apdu.getStatus() != 0x9000)
			System.out.println("Erreur lors de l'envoir du IC code.   SW= "+Integer.toHexString(apdu.getStatus()));
	}
	
	static void submitPIN(byte[] pincode) throws IOException, CadTransportException
	{
		Apdu apdu = new Apdu();
		apdu.command[Apdu.CLA] = myCLA;
		apdu.command[Apdu.INS] = INS_SUBMIT_PIN;
		apdu.command[Apdu.P1] = (byte)0x00;
		apdu.command[Apdu.P2] = (byte)0x00;
		apdu.setDataIn(pincode);
		cad.exchangeApdu(apdu);
		
		if(apdu.getStatus() != 0x9000)
			System.out.println("Erreur lors de l'envoir du PIN code.  SW= "+Integer.toHexString(apdu.getStatus()));
	}
	
	
	static void selectFile(byte[] fileid) throws IOException, CadTransportException
	{
		Apdu apdu = new Apdu();
		apdu.command[Apdu.CLA] = myCLA;
		apdu.command[Apdu.INS] = INS_SELECT_FILE;
		apdu.command[Apdu.P1] = (byte)0x00;
		apdu.command[Apdu.P2] = (byte)0x00;
		apdu.setDataIn(fileid);
		cad.exchangeApdu(apdu);
		
		if(apdu.getStatus() != 0x9000)
			System.out.println("Erreur lors de l'envoir du PIN code.  SW= "+Integer.toHexString(apdu.getStatus()));
		
	}
	
	static void writeContent(byte[] content) throws IOException, CadTransportException
	{
		Apdu apdu = new Apdu();
		apdu.command[Apdu.CLA] = myCLA;
		apdu.command[Apdu.INS] = INS_WRITE_FILE;
		apdu.command[Apdu.P1] = (byte)0x00;
		apdu.command[Apdu.P2] = (byte)0x00;
		apdu.setDataIn(content);
		cad.exchangeApdu(apdu);
		
		if(apdu.getStatus() != 0x9000)
			System.out.println("Erreur lors l'ecriture dan le fichier.  SW= "+Integer.toHexString(apdu.getStatus()));
	
	}
	
	static void readContent(byte offset, byte len) throws IOException, CadTransportException
	{
		Apdu apdu = new Apdu();
		apdu.command[Apdu.CLA] = myCLA;
		apdu.command[Apdu.INS] = INS_READ_FILE;
		apdu.command[Apdu.P1] = offset;  // OFFSET
		apdu.command[Apdu.P2] = len;  // 
		
		cad.exchangeApdu(apdu);
		
		if(apdu.getStatus() != 0x9000)
			System.out.println("Erreur lors de la lecture du fichier.  SW= "+Integer.toHexString(apdu.getStatus()));
		else
		{
			byte[] arr = apdu.getDataOut();
			System.out.println("Reading "+ len +" bytes, beginning from "+ offset + "  :");
			for (int i = 0; i < arr.length; i++) {
				//System.out.print(Integer.toHexString(arr[i]) + "  " );
				System.out.print((char)arr[i] + "  " );
			}
			System.out.println();
		}
	}
	
	

}
