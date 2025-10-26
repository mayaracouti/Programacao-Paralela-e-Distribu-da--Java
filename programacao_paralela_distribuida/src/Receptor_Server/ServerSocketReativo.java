package Receptor_Server;

import java.io.*;
import java.net.*;

/**
 * Servidor Receptor (R) - ESTE É O PROGRAMA PRINCIPAL.
 * - Porta fixa
 * - Aceita uma conexão, mantém aberta e trata objetos recebidos:
 * - Pedido -> executa contar(), envia Resposta
 * - ComunicadoEncerramento -> fecha conexão e volta a aceitar novas conexões
 */
public class ServerSocketReativo {

    private static final int PORTA = 12345;

    public static void main(String[] args) {
        System.out.println("[R] Iniciando servidor receptor na porta " + PORTA + "...");

        // Usamos try-with-resources para garantir que o ServerSocket feche no final
        try (ServerSocket socketServidor = new ServerSocket(PORTA)) {

            // Loop infinito para o servidor ficar sempre "vivo"
            while (true) {
                System.out.println("[R] Aguardando nova conexão...");

                // 1. ACEITA A CONEXÃO
                // O programa fica bloqueado aqui até um cliente D se conectar
                Socket socketCliente = socketServidor.accept();
                String enderecoCliente = socketCliente.getInetAddress().getHostAddress();
                System.out.println("[R] Conexão aceita de " + enderecoCliente + ":" + socketCliente.getPort());

                // Bloco try-with-resources para os streams e o socket do cliente
                // Eles fecharão automaticamente quando o loop 'continuarConectado' terminar
                try (
                        // 2. CRIA OS STREAMS DE OBJETOS
                        // Crie o ObjectInputStream primeiro no servidor, pois o cliente cria o ObjectOutStream primeiro.
                        ObjectInputStream receptorDeObjetos = new ObjectInputStream(socketCliente.getInputStream());
                        ObjectOutputStream transmissorDeObjetos = new ObjectOutputStream(socketCliente.getOutputStream())) {
                    // Damos 'flush' no transmissor para enviar o cabeçalho do stream
                    transmissorDeObjetos.flush();
                    System.out.println("[R] Streams de objetos criados com sucesso.");

                    boolean continuarConectado = true;
                    // 3. LOOP DA CONVERSA
                    // Fica neste loop enquanto a conexão com este cliente estiver ativa
                    while (continuarConectado) {

                        // 4. LÊ O OBJETO
                        // Fica bloqueado aqui esperando o cliente enviar algo
                        Object objetoRecebido = receptorDeObjetos.readObject();

                        // 5. PROCESSA O OBJETO RECEBIDO
                        if (objetoRecebido instanceof Pedido) {
                            Pedido pedido = (Pedido) objetoRecebido;
                            System.out.println("[R] Pedido recebido de " + enderecoCliente);

                            // 6. CHAMA A LÓGICA PARALELA
                            // O método .contar() está DENTRO de Pedido.java
                            int resultado = pedido.contar();
                            System.out.println("[R] Contagem paralela concluída: " + resultado);

                            // 7. ENVIA A RESPOSTA
                            Resposta resposta = new Resposta(resultado);
                            transmissorDeObjetos.writeObject(resposta);
                            transmissorDeObjetos.flush();
                            System.out.println("[R] Resposta enviada para " + enderecoCliente);

                        } else if (objetoRecebido instanceof ComunicadoEncerramento) {
                            System.out.println("[R] Comunicado de Encerramento recebido de " + enderecoCliente);
                            continuarConectado = false; // Sinaliza para sair do loop 'while(continuarConectado)'

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
                        socketCliente.close(); // Garante que o socket do cliente feche
                    } catch (IOException e) {
                        System.err.println("[R] Erro ao fechar o socket do cliente: " + e.getMessage());
                    }
                    System.out.println("[R] Conexão com " + enderecoCliente + " encerrada. Voltando a aceitar conexões.");
                }
            } // Fim do while(true) - volta a aceitar novas conexões
        } catch (IOException e) {
            System.err.println("[R] Não foi possível iniciar o servidor na porta " + PORTA + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}