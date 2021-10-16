package com.fcu.m1007888.iphw;

import java.net.*;
import java.util.StringTokenizer;
import java.io.*;

/**
 * Open an SMTP connection to a mailserver and send one mail.
 *
 */
public class SMTPConnection {
    /* The socket to the server */
    private Socket connection;

    /* Streams for reading and writing the socket */
    private BufferedReader fromServer;
    private DataOutputStream toServer;

    private static final int SMTP_PORT = 2525;
    private static final String CRLF = "\r\n";
    private static final String mailServer = "smtp.mailtrap.io";
    /*
     * https://mailtrap.io/
     * �K�O���l����ժA��
     */

    /* Are we connected? Used in close() to determine what to do. */
    private boolean isConnected = false;

    /* Create an SMTPConnection object. Create the socket and the 
       associated streams. Initialize SMTP connection. */
    public SMTPConnection(Envelope envelope) throws IOException {
    	/*
    	 * �@�~����
    	 * fromServer = new BufferedReader(new InputStreamReader(System.in));
    	 * toServer = System.out;
    	 */
    	System.out.println("Connecting to " + mailServer + " on port " + SMTP_PORT);
		connection = new Socket(mailServer, SMTP_PORT);
		fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		toServer = new DataOutputStream(connection.getOutputStream()); //�[�WDataOutputStream�ഫ�榡

		/* Read a line from server and check that the reply code is 220.
		   If not, throw an IOException. */
		String readFromSv = fromServer.readLine(); //���o�^�ǭ�
		if (parseReply(readFromSv) != 220) { //�q�^�ǭȴ���reply�N�X, �ˬd�O�_��220
			throw new IOException("Wrong reply code: " + readFromSv); //��ܨҥ~��reply
		}
		System.out.println("Connect to " + mailServer + " success");
	
		/* SMTP handshake. We need the name of the local machine.
		   Send the appropriate SMTP handshake command. */
		String localhost = "192.168.2.2"; //localhost ip
		System.out.println("Sending HELO: " + localhost);
		sendCommand("EHLO" + " " + localhost, 250); //tls�s�u�O�ϥ�EHLO
	
		isConnected = true;
    }

    /* Send the message. Write the correct SMTP-commands in the
       correct order. No checking for errors, just throw them to the
       caller. */
    //�Ӷ��ǿ�J���O, ���ε��ݦ��A���^��
    public void send(Envelope envelope) throws IOException {
		/* Send all the necessary commands to send a message. Call
		   sendCommand() to do the dirty work. Do _not_ catch the
		   exception thrown from sendCommand(). */
    	//�Ѧҧ�v��ch2 p55����
    	
    	/*
    	 * �}��TLS�s�u
    	 */
    	System.out.println("Sending AUTH LOGIN");
    	sendCommand("AUTH LOGIN", 334);
    	System.out.println("Enter username");
    	sendCommand("NTUzZGIxMTUxOTZiYzM=", 334); //base64�[�K
    	System.out.println("Enter password");
    	sendCommand("Njc3ZDhkMzQ1NmQ5MTY=", 235); //base64�[�K
    	
    	/*
    	 * �Ѿl�����O
    	 */
    	System.out.println("Sending MAIL FROM: " + envelope.Sender);
    	sendCommand("MAIL FROM:" + " " + "<" + envelope.Sender + ">", 250);
    	
    	System.out.println("Sending RCPT TO: " + envelope.Recipient);
    	sendCommand("RCPT TO:" + " " + "<" + envelope.Recipient + ">", 250);
    	
    	System.out.println("Sending DATA");
    	sendCommand("DATA", 354);
    	
    	System.out.println("Sending Message");
    	System.out.println(envelope.Message.toString());
    	sendCommand(envelope.Message.toString() + CRLF + ".", 250); // �T���̫�[�W���� & �y���ӵ���
    }

    /* Close the connection. First, terminate on SMTP level, then
       close the socket. */
    public void close() {
		isConnected = false;
		try {
			System.out.println("Sending QUIT");
		    sendCommand("QUIT", 221);
		    connection.close();
		} catch (IOException e) {
		    System.out.println("Unable to close connection: " + e);
		    isConnected = true;
		}
    }

    /* Send an SMTP command to the server. Check that the reply code is
       what is is supposed to be according to RFC 821. */
    private void sendCommand(String command, int rc) throws IOException {
		/* Write command to server and read reply from server. */
    	toServer.writeBytes(command + CRLF); //�[�W�^��+����Ÿ�
	
		/* Check that the server's reply code is the same as the parameter
		   rc. If not, throw an IOException. */
    	if (command.contains("EHLO")) {
    		boolean hasLine = false;
    		do {
    			hasLine = false;
    			String readFromSv = fromServer.readLine();
    	    	System.out.println("Server reply: " + readFromSv);
    	    	if(readFromSv.contains("-")) {
    	    		hasLine = true;
    	    	}
    		}while(hasLine); //���o�^�ǭ�, ����S��"-"
    	} else {
	    	String readFromSv = fromServer.readLine(); //���o�^�ǭ�
	    	System.out.println("Server reply: " + readFromSv);
    		if (parseReply(readFromSv) != rc) { //�q�^�ǭȴ���reply�N�X, �ˬd�O�_��rc
    			throw new IOException("Wrong reply code: " + readFromSv); //��ܨҥ~��reply
    		}
    	}
    }

    /* Parse the reply line from the server. Returns the reply code. */
    private int parseReply(String reply) {
    	/*
    	 * System.out.println("Reply: " + reply);
    	 * ���ճs�ugmail
    	 * �^�ǭ�:
    	 * 220 smtp.gmail.com ESMTP t38sm2569173pfg.102 - gsmtp
    	 * 
    	 * �ҥH���o�e����220�Y�i
    	 */
    	int rc = 0;
    	StringTokenizer st = new StringTokenizer(reply);
    	rc = Integer.parseInt(st.nextToken());
    	
    	return rc;
    }

    /* Destructor. Closes the connection if something bad happens. */
    /* 
     * MailClient�w�g��close connection, �o��O�O�I, �i�H�L��ĵ�i
     * 
     * �o�󪺭�]�Ѧ� @ https://www.ithome.com.tw/voice/129618
     * 
     * */
    @SuppressWarnings("deprecation")
	protected void finalize() throws Throwable {
		if(isConnected) {
		    close();
		}
		super.finalize();
    }
}