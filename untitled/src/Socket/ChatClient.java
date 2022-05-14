package Socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class ChatClient implements Runnable {
    private final Scanner scanner;
    private final Selector selector;
    private final SocketChannel clientChannel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);


    public static void main(String[] args) {
        try {
            ChatClient client = new ChatClient();
            client.start();
        } catch (IOException e) {
            System.err.println("Erro ao inicializar cliente: " + e.getMessage());
        }
    }

    public ChatClient() throws IOException {
        selector = Selector.open();
        clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);

        clientChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        clientChannel.connect(new InetSocketAddress(ChatServer.ADDRESS, ChatServer.PORT));
        scanner = new Scanner(System.in);
    }

    public void start() throws IOException {
        try {
            selector.select(1000);
            processConnectionAccept();

            new Thread(this).start();
            sendMessageLoop();
        }finally{
            clientChannel.close();
            selector.close();
        }
    }

    private void sendMessageLoop() throws IOException {
        String msg;
        do {
            System.out.print("Digite uma mensagem (ou sair para finalizar): ");
            msg = scanner.nextLine();
            clientChannel.write(ByteBuffer.wrap(msg.getBytes()));
        }while(!msg.equalsIgnoreCase("sair"));
    }

    private void processRead() throws IOException {
        buffer.clear();
        int bytesRead = clientChannel.read(buffer);

        buffer.flip();
        if (bytesRead > 0) {
            byte data[] = new byte[bytesRead];
            buffer.get(data);
            System.out.println("Mensagem recebida do servidor: " + new String(data));
        }
    }

    private void processConnectionAccept() throws IOException {
        System.out.println("Cliente conectado ao servidor");
        if(clientChannel.isConnectionPending()) {
            clientChannel.finishConnect();
        }
        System.out.print("Digite seu login: ");
        String login = scanner.nextLine();
        clientChannel.write(ByteBuffer.wrap(login.getBytes()));
    }

    @Override
    public void run() {
        try {
            while (selector.select(1000) > 0) {
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    if (selectionKey.isReadable())
                        processRead();
                    iterator.remove();
                }
            }
        }catch(IOException e){
            System.err.println("Erro ao ler dados enviados pelo servidor: " + e.getMessage());
        }
    }
}