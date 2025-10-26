package Receptores_Server;

/**
 * Resposta enviada pelo R contendo a contagem (int).
 */
public class Resposta extends Comunicado {
    private static final long serialVersionUID = 1L;

    private final int contagem;

    public Resposta(int contagem) {
        this.contagem = contagem;
    }

    public int getContagem() {return contagem;}
}
