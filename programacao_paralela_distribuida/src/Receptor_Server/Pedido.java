package Receptor_Server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Pedido extends Comunicado {
    private static final long serialVersionUID = 1L;
    
    private final byte[] numeros;
    private final byte procurado;

    public Pedido(byte[] numeros, byte procurado) {
        this.numeros = numeros;
        this.procurado = procurado;
    }

    public byte[] getNumeros() {
        return numeros;
    }

    public byte getProcurado() {
        return procurado;
    }
    public int contar() {
        if (numeros == null || numeros.length == 0) return 0;

        int processors = Runtime.getRuntime().availableProcessors();
        int length = numeros.length;

        if (length < 1000 || processors <= 1) {
            System.out.println("[R - Contar] Execução Sequencial (Vetor Pequeno ou 1 Core).");
            int c = 0;
            for (byte b : numeros) {
                if (b == procurado) c++;
            }
            return c;
        }

        System.out.println("[R - Contar] Execução Paralela com " + processors + " threads.");
        
        int nThreads = processors; 
        
        ExecutorService exec = Executors.newFixedThreadPool(nThreads);
        List<Future<Integer>> futures = new ArrayList<>();

        int TamanhoFragmento = (length + nThreads - 1) / nThreads;
        for (int i = 0; i < length; i += TamanhoFragmento) {
            final int start = i;
            final int end = Math.min(length, i + TamanhoFragmento);

            Callable<Integer> task = () -> {
                int localCount = 0;
     
                for (int j = start; j < end; j++) {
                    if (numeros[j] == procurado) localCount++;
                }
                return localCount;
            };

            futures.add(exec.submit(task));
        }

        int total = 0;
        try {
            for (Future<Integer> f : futures) {
                total += f.get(); 
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[R - Pedido.contar] Contagem interrompida: " + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("[R - Pedido.contar] Erro na execução de tarefa: " + e.getMessage());
        } finally {
            exec.shutdownNow(); 
        }

        return total;
    }
}