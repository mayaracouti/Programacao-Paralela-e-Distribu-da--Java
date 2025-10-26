package Distribuidor_client;

import Receptor_Server.ComunicadoEncerramento;
import Receptor_Server.Pedido;
import Receptor_Server.Resposta;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class DistribuidorApp {

    private static final Random RAND = new Random();

    public static void main(String[] args) {

        // Declara a variável fora do try
        int tamanho;

        // Scanner em try-with-resources
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Digite o tamanho do vetor: ");
            tamanho = sc.nextInt();
        }

        // Configuração do servidor
        String servidorIP = "127.0.0.1"; // IP do servidor
        int porta = 12344;               // Porta do servidor (igual ao do ServerSocketReativo)

        // Gera vetor aleatório
        byte[] vetor = new byte[tamanho];
        for (int i = 0; i < tamanho; i++) {
            vetor[i] = (byte) RAND.nextInt(101); // valores de 0 a 100
        }

        // Escolhe um byte aleatório para procurar
        byte procurado = vetor[RAND.nextInt(tamanho)];
        System.out.println("Número a ser contado: " + procurado);

        try (Socket socket = new Socket(servidorIP, porta);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Envia Pedido
            Pedido pedido = new Pedido(vetor, procurado);
            oos.writeObject(pedido);
            oos.flush();
            System.out.println("Pedido enviado ao servidor.");

            // Recebe Resposta
            Object obj = ois.readObject();
            if (obj instanceof Resposta resposta) {
                System.out.println("Contagem recebida do servidor: " + resposta.getContagem());
            } else {
                System.out.println("Objeto inesperado recebido: " + obj.getClass());
            }

            // Envia ComunicadoEncerramento
            oos.writeObject(new ComunicadoEncerramento());
            oos.flush();
            System.out.println("Comunicado de encerramento enviado.");

        } catch (Exception e) {
            System.err.println("Erro na comunicação com o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
