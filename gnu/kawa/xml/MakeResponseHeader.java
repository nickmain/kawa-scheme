// Copyright (c) 2002  Per M.A. Bothner and Brainfood Inc.
// This is free software;  for terms and warranty disclaimer see ./COPYING.

package gnu.kawa.xml;
import gnu.lists.*;
import gnu.mapping.*;

/** A procedure that implements the "response-header" function.
 * It is implemented by report an attribute objects.
 * Document-level attributes are otherwise not valid. */

public class MakeResponseHeader extends MethodProc
{
  public static MakeResponseHeader makeResponseHeader
    = new MakeResponseHeader();

  public void apply (CallContext ctx)
  {
    String key = ctx.getNextArg().toString();
    Object val = ctx.getNextArg();
    ctx.lastArg();
    Consumer out = ctx.consumer;
    out.beginAttribute(key, key);
    out.writeChars(val.toString());
    out.endAttribute();
  }

}
