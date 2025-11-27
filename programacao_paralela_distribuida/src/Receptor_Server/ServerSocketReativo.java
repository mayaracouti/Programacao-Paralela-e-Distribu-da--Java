package Receptor_Server;

import java.io.*;
import java.net.*;

public class ServerSocketReativo {

    private static final int PORTA_DEFAULT = 12344;

    public static void main(String[] args) {

        int PORTA;

        if (args.length == 0) {
            PORTA = PORTA_DEFAULT;
            System.err.println("[R] AVISO: Nenhuma porta especificada. Usando porta padrão " + PORTA + ".");
        } else {
            try {
                PORTA = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("[R] ERRO: Porta inválida '" + args[0] + "'. Encerrando.");
                return;
            }
        }

        System.out.println("[R] Iniciando servidor na porta " + PORTA + "...");
        
        // Bind em todas as interfaces (0.0.0.0) para aceitar conexões remotas
        try (ServerSocket servidor = new ServerSocket(PORTA, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("[R] Servidor aguardando conexões na porta " + PORTA + " (todas as interfaces)");
            System.out.println("[R] IP local: " + InetAddress.getLocalHost().getHostAddress());
            
            while (true) {
                Socket conexao = servidor.accept();
                System.out.println("[R] Conexão estabelecida com " + conexao.getInetAddress());

                try (ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
                     ObjectInputStream receptor = new ObjectInputStream(conexao.getInputStream())) {
                    
                    transmissor.flush();
                    boolean continuar = true;

                    while (continuar) {
                        Object recebido = receptor.readObject();    

                        if (recebido instanceof Pedido) {
                            Pedido pedido = (Pedido) recebido;
                            int contagem = pedido.contar();
                            transmissor.writeObject(new Resposta(contagem));
                            transmissor.flush();
                            System.out.println("[R] Pedido processado. Contagem = " + contagem);
                        } else if (recebido instanceof ComunicadoEncerramento) {
                            System.out.println("[R] Encerrando conexão com " + conexao.getInetAddress());
                            continuar = false;
                        }
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("[R] Erro ao desserializar objeto: " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("[R] Erro de E/S na comunicação: " + e.getMessage());
                } finally {
                    try {
                        if (!conexao.isClosed()) {
                            conexao.close();
                        }
                    } catch (IOException e) {
                        System.err.println("[R] Erro ao fechar conexão: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[R] Erro ao iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
