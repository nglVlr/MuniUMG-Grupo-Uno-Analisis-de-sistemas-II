package gt.muni.quejas.exception;

public class NegocioException extends RuntimeException{
    private final CodigoError codigoError;

    public NegocioException(CodigoError codigoError) {
        super(codigoError.getMensajePorDefecto());
        this.codigoError = codigoError;
    }

    public NegocioException(CodigoError codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }

    public CodigoError getCodigoError() {
        return codigoError;
    }
}
