package kawa.standard;
import kawa.lang.*;
import gnu.math.*;

/** Implement the standard Scheme procedure "asin". */

public class asin extends Procedure1
{
  public Object apply1 (Object arg1) throws WrongType
  {
    return new DFloNum (Math.asin (((RealNum)arg1).doubleValue ()));
  }
}
