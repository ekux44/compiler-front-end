
public class Project1 {

  public static void main(String[] args) {
    String filename = "sampleFromGrammar";
    
    
    Parser p = new Parser(Parser.getFile("input/"+filename+".pas"));
    
    while(true){
      if(p.getNextToken()==null)
        break;
    }
    p.writeListingFile("output/"+filename+".listing");
    p.writeTokenFile("output/"+filename+".token");
    
  }
  
}
