package kawa.lang;

import java.io.PrintStream;

public class Unquote extends Named implements Syntaxable {
   public kawa.lang.Unquote() {
      super("unquote");
   }

   public Object execute(kawa.lang.Interpreter i,java.util.Vector frames,Object arglist) 
      throws kawa.lang.WrongArguments
   {
      if (arglist instanceof kawa.lang.pair) {
         return ((kawa.lang.pair)arglist).car;
      } else {
         throw new kawa.lang.WrongArguments(this.name,1,"(unquote obj)");
      }
   }

   public void print(java.io.PrintStream ps) {
      ps.print("#<kawa.lang.Unquote>");
   }
}
