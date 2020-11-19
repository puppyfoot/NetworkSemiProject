package com.tcpip;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

import com.df.DataFrame;



public class Client {
	String ip;
	int port;
	Socket socket;
	Sender sender;
	Receiver receiver;
	String id;
	
	public Client() {
	}
	
	public Client(String ip, int port) {
		this.ip = ip;
		this.port = port;
		
	}
	public Client(String ip, int port, String id) {
		this.ip = ip;
		this.port = port;
		this.id = id;
		
	}
	
	
	public void connect() throws IOException {
		try {
			socket = new Socket(ip, port);
			System.out.println("Connection with Server Succeed");
		} catch (Exception e) {
			while(true) {
				try {
					Thread.sleep(2000);
					socket = new Socket(ip, port);
					break;
				}catch(Exception e1) {
					System.out.println("Connection with Server Retry...");
				}
			}
		}
		sender = new Sender(socket);
		new Receiver(socket).start();
	}
	
	
	// 데이터 전송 함수 -> 이 함수에 날아오는 센서값을 인자로 넣고 df.setContents로 넣어서 태블릿으로 보내야 합니다
	public void sendData(DataFrame df) {
//		while(true) {
			df.setIp("172.30.1.5");
			//Test용 Random 값 생성하여 setContents
			Random r = new Random();
			int val = r.nextInt(8)+15;
			String con = String.valueOf(val);
			df.setContents(con);

			System.out.println("data 전송: " + df.getIp());
			
			
			sender.setDf(df);
			System.out.println("[Client Sender Thread] Thread 생성");
			new Thread(sender).start();
//			try {
//				Thread.sleep(10000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//        }
//		}
	}
	
	public void sendData_test() {
		while(true) {
			DataFrame df = new DataFrame();
			df.setIp("172.30.1.5");
			df.setSender("carHead");
			//Test용 Random 값 생성하여 setContents
			Random r = new Random();
			int val = r.nextInt(8)+15;
			String con = String.valueOf(val);
			df.setContents(con);

			System.out.println("data 전송: " + df.getIp());
			System.out.println(df.getSender() + df.getContents());
			
			sender.setDf(df);
			System.out.println("[Client Sender Thread] Thread 전송");
			new Thread(sender).start();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	class Sender implements Runnable{
		
		Socket socket;
		ObjectOutputStream outstream;
		DataFrame df;
		
		public Sender(Socket socket) throws IOException {
			this.socket = socket;
			outstream = new ObjectOutputStream(socket.getOutputStream());
		}
		
		public void setDf(DataFrame df) {
			this.df = df;
		}

		@Override
		public void run() {
			//전송 outputStream이 비어 있지 않은 경우 실행!
			if(outstream != null) {
				// 전송 시도 
				try {
					System.out.println("[Client Sender Thread] 데이터 전송 시도: "+df.getIp()+"으로  "+df.getContents()+"전송");
					outstream.writeObject(df);
					outstream.flush();
					System.out.println("[Client Sender Thread] 데이터 전송 시도: "+df.getIp()+"으로 "+df.getContents()+"전송 완료");
				} catch (IOException e) {
					System.out.println("[Client Sender Thread] 전송 실패");
					// 전송 실패시 소켓이 열려 있다면 소켓 닫아버리고 다시 서버와 연결을 시도  
					try{
						if(socket != null) {
							System.out.println("[Client Sender Thread] 전송 실패, 소켓 닫음");
							socket.close();
						}
						// 소켓을 닫을 수 없음 
					}catch(Exception e1) {
						e1.printStackTrace();
					}
					// 다시 서버와 연결 시도
					try {
						Thread.sleep(2000);
						connect();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}
	
	
	class Receiver extends Thread{
		
		ObjectInputStream oi;
		
		public Receiver(Socket socket) throws IOException {
			oi = new ObjectInputStream(socket.getInputStream());
		}

		@Override
		public void run() {
			// 수신 inputStream이 비어 있지 않은 경우 실행!
			while(oi != null) {
				DataFrame df = null;
				// 수신 시도
				try {
					System.out.println("[Client Receiver Thread] 수신 대기");
					df = (DataFrame)oi.readObject();
					System.out.println("[Client Receiver Thread] 수신 완료");
					System.out.println(df.getSender()+": "+df.getContents());
					
				} catch (Exception e) {
					System.out.println("[Client Receiver Thread] 수신 실패");
					e.printStackTrace();
					break;
				}
			}
			// 수신 실패후 ObjectInputStream닫고 소켓이 열려 있다면 소켓 닫기
			try {
				if(oi != null) {
					oi.close();
				}
				if(socket != null) {
					socket.close();
				}
			}catch(Exception e) {
				
			}
		}
	}


	public static void main(String[] args) {

		Client client = new Client("192.168.0.66",5558,"CarHead");

		try {
			client.connect();
//			client.sendData_test();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
