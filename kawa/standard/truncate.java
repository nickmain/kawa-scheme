package kawa.standard;
import kawa.lang.*;
import kawa.math.*;

/** Implement the standard Scheme procedure "trunctate". */

public class truncate extends Procedure1
{
   public Object apply1 (Object arg1)
   {
     return ((RealNum)arg1).toInt (Numeric.TRUNCATE);
   }
}
