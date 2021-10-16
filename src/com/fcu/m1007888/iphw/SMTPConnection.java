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
     * 免費的郵件測試服務
     */

    /* Are we connected? Used in close() to determine what to do. */
    private boolean isConnected = false;

    /* Create an SMTPConnection object. Create the socket and the 
       associated streams. Initialize SMTP connection. */
    public SMTPConnection(Envelope envelope) throws IOException {
    	/*
    	 * 作業提示
    	 * fromServer = new BufferedReader(new InputStreamReader(System.in));
    	 * toServer = System.out;
    	 */
    	System.out.println("Connecting to " + mailServer + " on port " + SMTP_PORT);
		connection = new Socket(mailServer, SMTP_PORT);
		fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		toServer = new DataOutputStream(connection.getOutputStream()); //加上DataOutputStream轉換格式

		/* Read a line from server and check that the reply code is 220.
		   If not, throw an IOException. */
		String readFromSv = fromServer.readLine(); //取得回傳值
		if (parseReply(readFromSv) != 220) { //從回傳值提取reply代碼, 檢查是否為220
			throw new IOException("Wrong reply code: " + readFromSv); //顯示例外的reply
		}
		System.out.println("Connect to " + mailServer + " success");
	
		/* SMTP handshake. We need the name of the local machine.
		   Send the appropriate SMTP handshake command. */
		String localhost = "192.168.2.2"; //localhost ip
		System.out.println("Sending HELO: " + localhost);
		sendCommand("EHLO" + " " + localhost, 250); //tls連線是使用EHLO
	
		isConnected = true;
    }

    /* Send the message. Write the correct SMTP-commands in the
       correct order. No checking for errors, just throw them to the
       caller. */
    //照順序輸入指令, 不用等待伺服器回傳
    public void send(Envelope envelope) throws IOException {
		/* Send all the necessary commands to send a message. Call
		   sendCommand() to do the dirty work. Do _not_ catch the
		   exception thrown from sendCommand(). */
    	//參考投影片ch2 p55順序
    	
    	/*
    	 * 開啟TLS連線
    	 */
    	System.out.println("Sending AUTH LOGIN");
    	sendCommand("AUTH LOGIN", 334);
    	System.out.println("Enter username");
    	sendCommand("NTUzZGIxMTUxOTZiYzM=", 334); //base64加密
    	System.out.println("Enter password");
    	sendCommand("Njc3ZDhkMzQ1NmQ5MTY=", 235); //base64加密
    	
    	/*
    	 * 剩餘的指令
    	 */
    	System.out.println("Sending MAIL FROM: " + envelope.Sender);
    	sendCommand("MAIL FROM:" + " " + "<" + envelope.Sender + ">", 250);
    	
    	System.out.println("Sending RCPT TO: " + envelope.Recipient);
    	sendCommand("RCPT TO:" + " " + "<" + envelope.Recipient + ">", 250);
    	
    	System.out.println("Sending DATA");
    	sendCommand("DATA", 354);
    	
    	System.out.println("Sending Message");
    	System.out.println(envelope.Message.toString());
    	sendCommand(envelope.Message.toString() + CRLF + ".", 250); // 訊息最後加上換行 & 句號來結束
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
    	toServer.writeBytes(command + CRLF); //加上回車+換行符號
	
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
    		}while(hasLine); //取得回傳值, 直到沒有"-"
    	} else {
	    	String readFromSv = fromServer.readLine(); //取得回傳值
	    	System.out.println("Server reply: " + readFromSv);
    		if (parseReply(readFromSv) != rc) { //從回傳值提取reply代碼, 檢查是否為rc
    			throw new IOException("Wrong reply code: " + readFromSv); //顯示例外的reply
    		}
    	}
    }

    /* Parse the reply line from the server. Returns the reply code. */
    private int parseReply(String reply) {
    	/*
    	 * System.out.println("Reply: " + reply);
    	 * 測試連線gmail
    	 * 回傳值:
    	 * 220 smtp.gmail.com ESMTP t38sm2569173pfg.102 - gsmtp
    	 * 
    	 * 所以取得前面的220即可
    	 */
    	int rc = 0;
    	StringTokenizer st = new StringTokenizer(reply);
    	rc = Integer.parseInt(st.nextToken());
    	
    	return rc;
    }

    /* Destructor. Closes the connection if something bad happens. */
    /* 
     * MailClient已經有close connection, 這邊是保險, 可以無視警告
     * 
     * 廢棄的原因參考 @ https://www.ithome.com.tw/voice/129618
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