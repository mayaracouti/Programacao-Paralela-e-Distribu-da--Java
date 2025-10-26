package Receptores_Server;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/**
 * Pedido contém o vetor de bytes e o byte procurado.
 * Implementa o método contar() que realiza a contagem em paralelo.
 */
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

    /**
     * Conta quantas vezes 'procurado' aparece em 'numeros'.
     * Implementado usando um ExecutorService com até availableProcessors() threads.
     * Retorna um int com a contagem total.
     */
    public int contar() {
        if (numeros == null || numeros.length == 0) return 0;

        /*Descobre quantos núcleos o PC do Receptor tem */
        int processors = Runtime.getRuntime().availableProcessors();

        /*verifica se vet é pequeno */
        int length = numeros.length;

        // se vetor muito pequeno, faz contagem sequencial
        if (length < 1000 || processors <= 1) {
            int c = 0;
            for (byte b : numeros) {
                if (b == procurado) c++;
            }
            return c;
        }


        /*CRIA UM "POOL" DE THREADS com número de threads  igual ao nº de nucleos do PC receptor */
        /*Garante que não teremos mais threads do que itens no vetor (ou nucleos) */
        int nThreads = Math.min(processors, length);
        ExecutorService exec = Executors.newFixedThreadPool(nThreads); /*Cria uma equipa de nThreads (ex: qtd achada anteriormente) trabalhadoras e mantenha-as prontas para trabalhar */
        List<Future<Integer>> futures = new ArrayList<>();

        /*DIVIDE O TRABALHO calculando o tamanho que cada thread ira processar */
        int TamanhoFragmento = (length + nThreads - 1) / nThreads;
        for (int i = 0; i < length; i += TamanhoFragmento) {
            final int start = i;
            final int end = Math.min(length, i + TamanhoFragmento);

            /*CRIA A TAREFA PARA UMA THREAD */
            Callable<Integer> task = () -> {
                int localCount = 0;
                for (int j = start; j < end; j++) {
                    if (numeros[j] == procurado) localCount++;
                }
                return localCount;
            };

            /* adiciona numa das threads livres e executa a tarefa */
            futures.add(exec.submit(task));
        }

        /*JUNTA OS RESULTADOS - O servidor espera que todas as threads terminem */
        int total = 0;
        try {
            for (Future<Integer> f : futures) {
                total += f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Pedido.contar] contagem interrompida: " + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("[Pedido.contar] erro na execução de tarefa: " + e.getMessage());
        } finally {
            exec.shutdownNow();
        }

        return total;
    }
}
