package Socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ChatServer {
    public static final int PORT = 4000;
    public static final String ADDRESS = "127.0.0.1";

    private final Selector selector;
    private final ServerSocketChannel serverChannel;

    private final ByteBuffer buffer;

    public static void main(String[] args) {
        //Define o número máximo de threads a serem criadas usando Stream do Java 8 (opcional)
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");

        System.out.printf("CPUs: %d | Max Threads usadas por Streams do Java 8: %s\n\n",
                Runtime.getRuntime().availableProcessors(),
                System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));

        try {
            ChatServer server = new ChatServer();
            server.start();
        } catch (IOException e) {
            System.err.println("Erro durante execução do servidor: " + e.getMessage());
        }
    }

    public ChatServer() throws IOException {
        buffer = ByteBuffer.allocate(1024);
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        serverChannel.bind(new InetSocketAddress(ADDRESS, PORT), 10000);
        System.out.println("Servidor de chat não-bloqueante iniciado no endereço " + ADDRESS + " na porta " + PORT);
    }

    public void start() {
        while(true) {
            try {
                selector.select();
                processEvents(selector.selectedKeys());
            } catch (IOException e){
                System.err.println(e.getMessage());
            }
        }
    }

    private void processEvents(Set<SelectionKey> selectionKeys) {
        selectionKeys.stream().parallel().forEach(this::processEvent);
        selectionKeys.clear();
    }

    private void processEvent(SelectionKey selectionKey){
        if (!selectionKey.isValid()) {
            return;
        }

        try{
            processConnectionAccept(selectionKey, selector);
            processRead(selectionKey);
        }catch(IOException ex){
            System.out.println("Erro ao processar evento: " + ex.getMessage());
        }
    }

    private void processConnectionAccept(SelectionKey key, Selector selector) throws IOException {
        if (!key.isAcceptable()) {
            return;
        }
        SocketChannel clientChannel = serverChannel.accept();
        System.out.println("Cliente " + clientChannel.getRemoteAddress() + " conectado.");
        clientChannel.configureBlocking(false);
        clientChannel.write(ByteBuffer.wrap("Bem vindo ao chat.\n".getBytes()));
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private void processRead(SelectionKey selectionKey) throws IOException {
        if(!selectionKey.isReadable()){
            return;
        }

        SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
        buffer.clear();

        int bytesRead;
        try {
            bytesRead = clientChannel.read(buffer);
        } catch (IOException e) {
            System.err.println(
                    "Não pode ler dados. Conexão fechada pelo cliente " +
                            clientChannel.getRemoteAddress() + ": " + e.getMessage());
            clientChannel.close();
            selectionKey.cancel();
            return;
        }

        if(bytesRead <= 0){
            return;
        }

        buffer.flip();
        byte[] data = new byte[bytesRead];
        buffer.get(data);
        System.out.println(
                "Mensagem recebida do cliente " +
                        clientChannel.getRemoteAddress() + ": " + new String(data) +
                        " (" + bytesRead + " bytes lidos)");
    }
}