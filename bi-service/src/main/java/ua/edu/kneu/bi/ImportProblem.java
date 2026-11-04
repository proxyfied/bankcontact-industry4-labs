package ua.edu.kneu.bi;
public class ImportProblem extends RuntimeException {
 private final int status;
 public ImportProblem(int status,String message) { super(message); this.status=status; }
 public int status() { return status; }
}
