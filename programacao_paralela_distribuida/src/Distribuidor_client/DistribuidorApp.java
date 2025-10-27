package Distribuidor_client;

import Receptor_Server.ComunicadoEncerramento;
import Receptor_Server.Pedido;
import Receptor_Server.Resposta;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.Arrays; 

public class DistribuidorApp {

    private static final Random RAND = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String[] servidores = { "127.0.0.1", "127.0.0.1","172.16.232.48" };
        int[] portas = { 12344, 12345 };

        System.out.print("Digite o tamanho do vetor: ");
        int tamanho = sc.nextInt();
        sc.nextLine(); 

        byte[] vetor = new byte[tamanho];
        for (int i = 0; i < tamanho; i++) {
            vetor[i] = (byte) (RAND.nextInt(201) - 100);
        }

        System.out.print("Deseja imprimir o vetor grande para conferência? (s/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("s")) {
            System.out.println("Vetor gerado: " + Arrays.toString(vetor));
        }

        byte procurado;
        System.out.print("Deseja procurar um número aleatório (a) ou o número 111 (para testar 0 ocorrências)? (a/111): ");
        String escolha = sc.nextLine().trim();

        if ("111".equalsIgnoreCase(escolha)) {
            procurado = 111; 
            System.out.println("[D] Número a ser contado (inexistente): " + procurado);
        } else {
            procurado = vetor[RAND.nextInt(tamanho)];
            System.out.println("[D] Número a ser contado (aleatório): " + procurado);
        }

        int partes = servidores.length;
        int parteTamanho = tamanho / partes;

        Thread[] threads = new Thread[partes];
        int[] resultados = new int[partes];
        
        long inicio = System.currentTimeMillis();

        for (int i = 0; i < partes; i++) {
            final int idx = i;
            final String ip = servidores[i];
            final int porta = portas[i];
            final int inicioParte = idx * parteTamanho;
            final int fimParte = (idx == partes - 1) ? tamanho : inicioParte + parteTamanho;

            threads[i] = new Thread(() -> {
                try (Socket socket = new Socket(ip, porta);
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                    byte[] subVetor = new byte[fimParte - inicioParte];
                    System.arraycopy(vetor, inicioParte, subVetor, 0, subVetor.length);

                    Pedido pedido = new Pedido(subVetor, procurado);
                    oos.writeObject(pedido);
                    oos.flush();
                    System.out.println("[D] Enviando Pedido para " + ip + ":" + porta + ". Tamanho da parte: " + subVetor.length);

                    Object obj = ois.readObject();
                    if (obj instanceof Resposta) {
                        Resposta resposta = (Resposta) obj;
                        resultados[idx] = resposta.getContagem();
                        System.out.println(
                                "[D] Resposta recebida de " + ip + ":" + porta + " -> Contagem parcial: " + resposta.getContagem());
                    } else {
                        System.err.println("[D] Objeto inesperado recebido de " + ip + ":" + porta);
                    }

                    oos.writeObject(new ComunicadoEncerramento());
                    oos.flush();
                    System.out.println("[D] Comunicado de encerramento enviado para " + ip + ":" + porta);

                } catch (IOException e) {
                    System.err.println("[D] Erro de E/S (Conexão/Comunicação) em " + ip + ":" + porta + ": " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    System.err.println("[D] Erro ao desserializar objeto de " + ip + ":" + porta + ": Classe não encontrada.");
                } catch (Exception e) {
                    System.err.println("[D] Erro inesperado na comunicação com " + ip + ":" + porta + ": " + e.getMessage());
                }
            });

            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.err.println("[D] A espera foi interrompida.");
                Thread.currentThread().interrupt();
            }
        }

        long fim = System.currentTimeMillis();

        int total = 0;
        for (int r : resultados) {
            total += r;
        }

        System.out.println("\n--- RESULTADO FINAL ---");
        System.out.println("[D] Número procurado: " + procurado);
        System.out.println("[D] Contagem total do número " + procurado + ": " + total);
        System.out.printf("[D] Tempo de execução paralelo e distribuído: %.2f segundos%n", (fim - inicio) / 1000.0);
        System.out.println("-----------------------");
        
        sc.close();
    }
}