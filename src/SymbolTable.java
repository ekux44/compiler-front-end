/**
 * @author Eric Kuxhausen
 */
import java.util.HashMap;

public class SymbolTable {

  public HashMap<String, Token> table;

  public SymbolTable() {
    table = new HashMap<String, Token>();
  }
}
