import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
	private ServerSocket server = null;
	private Socket socketClient = null;
	private int porta = 1234;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Transfer statoClient;
	private FroggerModel clientModel = new FroggerModel(0);
	private PnlFrog clientView;
	private boolean first = true;
	private JFrame clientFrame;
	
	private FroggerCtrl ctrl;
	
	public Server(FroggerCtrl ctrl) throws IOException
	{
		this.ctrl = ctrl;
		/*try
		{
			this.clientView = new PnlFrog(ctrl);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}*/
	}
	
	Thread ricezione = new Thread(new Runnable()
	{
		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					statoClient = (Transfer) in.readObject();
					clientModel.transferToModel(statoClient);
					if (first)
					{
						clientView = new PnlFrog(clientModel);
						first = false;
						clientView.ctrl=ctrl;
						clientView.state = PnlFrog.STATE.GAME;
						newWindow();
					}
					clientView.setEntities(clientModel.entities);
					clientView.repaint();
				}
				/*catch (NullPointerException e)
				{
					//In caso il server non riceve nulla si aspetta che arrivi qualcosa senza gestire alcuna eccezione
				}*/
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					System.out.println("CONNESSIONE INTERROTTA");
					System.out.println(e);
					System.exit(0);
				}
			}
		}
	});
	
	
	public Socket connessione()
	{
		try
		{
			System.out.println("\nDati per la connessione:\nIP: " + InetAddress.getLocalHost().getHostAddress() + "\tPORTA: 1234\n");
			System.out.println("[0] - Inizializzo il server...");
			server = new ServerSocket(porta);
			System.out.println("[1] - Server pronto, in ascolto sulla porta " + porta);
			socketClient = server.accept();
			System.out.println("[2] - Connessione riuscita con il client");
			server.close();
			InputStream inputStream = socketClient.getInputStream();
			OutputStream outputStream = socketClient.getOutputStream();
			out = new ObjectOutputStream(outputStream);
			in = new ObjectInputStream(inputStream);
			ricezione.start();
			//newWindow();
		}
		catch (IOException e)
		{
			System.out.println("CONNESSIONE NON RIUSCITA!");
			System.out.println(e);
		}
		return socketClient;
	}
	
	public void newWindow()
	{
		clientFrame = new JFrame();
		clientFrame.setBounds(500, 0, 656, 1000);
		clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		clientFrame.setLayout(new BorderLayout());
		clientFrame.setTitle("ClientView");
		
		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(Color.WHITE);
		
		clientFrame.add(clientView);
		
		clientFrame.setVisible(true);
	}
	
	public void send()
	{
		Transfer statoCorrente = ctrl.modelToTransfer(ctrl.model);
        try
        {
            out.writeObject(statoCorrente);
            out.reset();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}