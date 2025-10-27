package Receptor_Server;

import java.io.*;
import java.net.*;

public class ServerSocketReativo {

    private static final int PORTA_DEFAULT = 12344;

    public static void main(String[] args) {

        int PORTA;
        
        if (args.length == 0) {
            PORTA = PORTA_DEFAULT;
            System.err.println("AVISO: Nenhuma porta especificada. Usando porta padrão " + PORTA + ".");
        } else {
            try {
                PORTA = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("ERRO: O argumento '" + args[0] + "' não é um número de porta válido. Encerrando.");
                return;
            }
        }

        System.out.println("[R] Iniciando servidor receptor na porta " + PORTA + "...");

                try (ServerSocket socketServidor = new ServerSocket(PORTA)) {

                        while(true) {
                System.out.println("\n[R] Aguardando nova conexão...");
                Socket socketCliente = socketServidor.accept();
                String enderecoCliente = socketCliente.getInetAddress().getHostAddress();
                System.out.println("[R] Conexão aceita de " + enderecoCliente + ":" + socketCliente.getPort());

                try (
                        ObjectOutputStream transmissorDeObjetos = new ObjectOutputStream(socketCliente.getOutputStream());
                        ObjectInputStream receptorDeObjetos = new ObjectInputStream(socketCliente.getInputStream())) {
                    
                    transmissorDeObjetos.flush();
                    System.out.println("[R] Streams de objetos criados com sucesso.");

                    boolean continuarConectado = true;
                    
                    while(continuarConectado) {

                                                Object objetoRecebido = receptorDeObjetos.readObject();

                                                if (objetoRecebido instanceof                             Pedido pedido) {
                            System.out.println("[R] Pedido recebido de " + enderecoCliente);

                                                        int resultado = pedido.contar();
                            System.out.println("[R] Contagem paralela concluída: " + resultado);

                                                        Resposta resposta = new Resposta(resultado);
                            transmissorDeObjetos.writeObject(resposta);
                            transmissorDeObjetos.flush();
                            System.out.println("[R] Resposta enviada para " + enderecoCliente);

                        } else if (objetoRecebido instanceof ComunicadoEncerramento) {
                            System.out.println("[R] Comunicado de Encerramento recebido de " + enderecoCliente);
                            continuarConectado = false;
                        } else {
                            System.out.println("[R] Objeto desconhecido recebido: " + objetoRecebido.getClass().getName());
                        }
                    }
                } catch (EOFException eof) {
                    System.out.println("[R] Cliente " + enderecoCliente + " desconectou abruptamente.");
                } catch (ClassNotFoundException cnf) {
                    System.err.println("[R] Erro ao desserializar objeto: " + cnf.getMessage());
                } catch (IOException e) {
                    System.err.println("[R] Erro de E/S na comunicação com " + enderecoCliente + ": " + e.getMessage());
                                    } finally {
                    try {
if (socketCliente != null && !socketCliente.isClosed()) {
                        socketCliente.close(); 
                        }
                    } catch (IOException e) {
                        System.err.println("[R] Erro ao fechar o socket do cliente: " + e.getMessage());
                    }
                    System.out.println("[R] Conexão com " + enderecoCliente + " encerrada. Voltando a aceitar conexões.");
                }
            }
        } catch (IOException e) {
            System.err.println("[R] Não foi possível iniciar o servidor na porta " + PORTA + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}