package Distribuidor_client;

import Receptor_Server.ComunicadoEncerramento;
import Receptor_Server.Pedido;
import Receptor_Server.Resposta;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.Scanner;
import java.util.Arrays;

public class DistribuidorApp {

    private static final Random RAND = new Random();
    private static final int TIMEOUT_CONEXAO = 5000; // 5 segundos
    private static final int TIMEOUT_OPERACAO = 30000; // 30 segundos

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // MODO TESTE LOCAL: usando localhost com portas diferentes
        String[] servidores = { "172.16.234.165", "172.16.232.131" };
        int[] portas = { 12344, 12344 }; // DUAS PORTAS DIFERENTES!

        // MODO DISTRIBUÍDO: descomente quando os servidores remotos estiverem rodando
        // String[] servidores = { "172.16.225.168", "172.16.232.131" };
        // int[] portas = { 12344, 12344 }; // Mesma porta em máquinas diferentes

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
        System.out.print(
                "Deseja procurar um número aleatório (a) ou o número 111 (para testar 0 ocorrências)? (a/111): ");
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
        boolean[] sucessos = new boolean[partes];

        System.out.println("\n[D] Iniciando distribuição paralela...");
        System.out.println("[D] Servidores: " + Arrays.toString(servidores));
        System.out.println("[D] Portas: " + Arrays.toString(portas));
        System.out.println("[D] Partes: " + partes + " (tamanho de cada parte: ~" + parteTamanho + ")\n");

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < partes; i++) {
            final int idx = i;
            final String ip = servidores[i];
            final int portaUsada = portas[idx]; // USAR A PORTA CORRETA PARA CADA SERVIDOR
            final int inicioParte = idx * parteTamanho;
            final int fimParte = (idx == partes - 1) ? tamanho : inicioParte + parteTamanho;

            threads[i] = new Thread(() -> {
                Socket socket = null;
                long tempoInicio = System.currentTimeMillis();

                try {
                    System.out.println("[D] Thread " + idx + " tentando conectar em " + ip + ":" + portaUsada + "...");

                    socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, portaUsada), TIMEOUT_CONEXAO);
                    socket.setSoTimeout(TIMEOUT_OPERACAO);

                    long tempoConexao = System.currentTimeMillis() - tempoInicio;
                    System.out.println("[D] Thread " + idx + " conectada com SUCESSO em " + ip + ":" + portaUsada +
                            " (tempo: " + tempoConexao + "ms)");

                    try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                        oos.flush();

                        byte[] subVetor = new byte[fimParte - inicioParte];
                        System.arraycopy(vetor, inicioParte, subVetor, 0, subVetor.length);

                        Pedido pedido = new Pedido(subVetor, procurado);
                        oos.writeObject(pedido);
                        oos.flush();
                        System.out.println("[D] Thread " + idx + " enviou Pedido para " + ip + ":" + portaUsada +
                                ". Tamanho da parte: " + subVetor.length);

                        Object obj = ois.readObject();
                        if (obj instanceof Resposta) {
                            Resposta resposta = (Resposta) obj;
                            resultados[idx] = resposta.getContagem();
                            sucessos[idx] = true;
                            System.out.println("[D] Thread " + idx + " recebeu Resposta de " + ip + ":" + portaUsada +
                                    " -> Contagem parcial: " + resposta.getContagem());
                        } else {
                            System.err.println(
                                    "[D] Thread " + idx + " recebeu objeto inesperado de " + ip + ":" + portaUsada);
                        }

                        oos.writeObject(new ComunicadoEncerramento());
                        oos.flush();
                        System.out.println("[D] Thread " + idx + " enviou Comunicado de encerramento para " + ip + ":"
                                + portaUsada);

                    }

                } catch (SocketTimeoutException e) {
                    long tempoTotal = System.currentTimeMillis() - tempoInicio;
                    System.err.println(
                            "[D] Thread " + idx + " - TIMEOUT ao conectar/comunicar com " + ip + ":" + portaUsada +
                                    " (tempo decorrido: " + tempoTotal + "ms)");
                    System.err.println("[D] ⚠  VERIFIQUE SE O SERVIDOR ESTÁ RODANDO EM " + ip + ":" + portaUsada);

                } catch (IOException e) {
                    System.err.println(
                            "[D] Thread " + idx + " - Erro de E/S em " + ip + ":" + portaUsada + ": " + e.getMessage());
                    System.err.println(
                            "[D] Possíveis causas: servidor offline, firewall bloqueando, IP/porta incorretos");

                } catch (ClassNotFoundException e) {
                    System.err.println(
                            "[D] Thread " + idx + " - Erro ao desserializar objeto de " + ip + ":" + portaUsada);

                } catch (Exception e) {
                    System.err.println("[D] Thread " + idx + " - Erro inesperado com " + ip + ":" + portaUsada + ": "
                            + e.getMessage());
                    e.printStackTrace();

                } finally {
                    if (socket != null && !socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.err.println("[D] Thread " + idx + " - Erro ao fechar socket: " + e.getMessage());
                        }
                    }
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
        int servidoresResponsivos = 0;
        for (int i = 0; i < resultados.length; i++) {
            total += resultados[i];
            if (sucessos[i]) {
                servidoresResponsivos++;
            }
        }

        System.out.println("\n========================================");
        System.out.println("         RESULTADO FINAL");
        System.out.println("========================================");
        System.out.println("[D] Número procurado: " + procurado);
        System.out.println("[D] Contagem total: " + total);
        System.out.println("[D] Servidores que responderam: " + servidoresResponsivos + "/" + partes);

        if (servidoresResponsivos < partes) {
            System.out.println("\n⚠  AVISO: Alguns servidores não responderam!");
            System.out.println("⚠  O resultado pode estar INCOMPLETO!");
            System.out.println("⚠  Servidores com problema:");
            for (int i = 0; i < sucessos.length; i++) {
                if (!sucessos[i]) {
                    System.out.println("    - " + servidores[i] + ":" + portas[i]);
                }
            }
        } else {
            System.out.println("✅ Todos os servidores responderam com sucesso!");
        }

        System.out.printf("\n[D] Tempo total: %.2f segundos%n", (fim - inicio) / 1000.0);
        System.out.println("========================================\n");

        sc.close();
    }
}
