import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket server = null;
    private Socket socketClient = null;
    private int porta = 1234;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private FroggerModel model;
    private PnlFrog clientView;
    private boolean first = true;
    private JFrame clientFrame;


    Thread ricezione = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    model=(FroggerModel) in.readObject();
                    if (first)
                    {
                        clientView = new PnlFrog(model);
                        first = false;
                    }
                    clientView.repaint();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    System.out.println("CONNESSIONE INTERROTTA");
                    System.exit(0);
                }
            }
        }
    });


    public Socket connessione()
    {
        try
        {
            System.out.println("\nDati per la connessione:\nIP: "+ InetAddress.getLocalHost().getHostAddress()+"\tPORTA: 6789\n");
            System.out.println("[0] - Inizializzo il server...");
            server = new ServerSocket(porta);
            System.out.println("[1] - Server pronto, in ascolto sulla porta "+porta);
            socketClient = server.accept();
            System.out.println("[2] - Connessione riuscita con il client");
            server.close();
            InputStream inputStream = socketClient.getInputStream();
            OutputStream outputStream = socketClient.getOutputStream();
            out = new ObjectOutputStream(outputStream);
            in = new ObjectInputStream(inputStream);
            ricezione.start();
            newWindow();
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

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);

        clientFrame.add(clientView);
    }

    public void send(FroggerModel model) throws IOException {
        out.writeObject(model);
        out.reset();
    }
}