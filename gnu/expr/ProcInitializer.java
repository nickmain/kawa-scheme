package gnu.expr;
import gnu.bytecode.*;
import gnu.mapping.*;

public class ProcInitializer extends Initializer
{
    LambdaExp proc;

    /* #ifndef use:java.lang.invoke */
    // static final Method lookupApplyHandleMethod =
    //     Compilation.typeProcedure.getDeclaredMethod("lookupApplyHandle", 2);
    /* #endif */

    public ProcInitializer(LambdaExp lexp, Compilation comp, Field field) {
        this.field = field;
        proc = lexp;
        LambdaExp heapLambda = field.getStaticFlag() ? comp.getModule()
            : lexp.getOwningLambda();
        if (field.getStaticFlag()) {
            next = comp.clinitChain;
            comp.clinitChain = this;
        } else {
            next = heapLambda.initChain;
            heapLambda.initChain = this;
        }
    }

    /** Create and load a CompiledProc for the given procedure. */
    public static void emitLoadModuleMethod(LambdaExp proc, Compilation comp) {
        Declaration pdecl = proc.nameDecl;
        Object pname = pdecl == null ? proc.getName() : pdecl.getSymbol();
        CompiledProc oldproc = null;
        if (comp.isInteractive() && pname != null
            && pdecl != null && pdecl.context instanceof ModuleExp) {
            // In interactive mode allow dynamic rebinding of procedures.
            // If there is an existing CompiledProc binding, re-use it.
            ModuleInfo minfo = comp.getMinfo();
            Class oldClass = minfo.getOldModuleClass();
            if (oldClass != null && pdecl.getField() != null) {
                try {
                    Object oldpval = oldClass
                        .getField(pdecl.getField().getName()).get(null);
                    if (oldpval instanceof CompiledProc)
                        oldproc = (CompiledProc) oldpval;
                } catch (Throwable ex) {
                }
            }
            if (oldproc == null) {
                Environment env = Environment.getCurrent();
                Symbol sym = pname instanceof Symbol ? (Symbol) pname
                    : Symbol.make("", pname.toString().intern());
                Object property = comp.getLanguage().getEnvPropertyFor(proc.nameDecl);
                Object old = env.get(sym, property, null);
                if (old instanceof CompiledProc) {
                    String moduleName =
                        ((CompiledProc) old).getModuleClass().getName();
                    if (moduleName.startsWith(ModuleManager.interactiveClassPrefix)
                        || moduleName.equals(comp.moduleClass.getName()))
                        oldproc = (CompiledProc) old;
                }
            }
        }
        CodeAttr code = comp.getCode();
        ClassType procClass = Compilation.typeCompiledProc;
        Method initModuleMethod;
        int appArgs = 4;
        String name;
        if (oldproc == null) {
            name = proc.usingCallContext() ? "makeResultToConsumer"
                : "makeResultToObject";
        } else {
            name = proc.usingCallContext() ? "initResultToConsumer"
                : "initResultToObject";
            comp.compileConstant(oldproc, Target.pushValue(procClass));
            code.emitDup();
            //initModuleMethod = Compilation.typeModuleMethod.getDeclaredMethod("init", appArgs);
        }
        initModuleMethod = procClass.getDeclaredMethod(name, appArgs);
        LambdaExp owning = proc.getNeedsClosureEnv() ? proc.getOwningLambda()
            : comp.getModule();
        if (owning instanceof ClassExp && owning.staticLinkField != null)
            code.emitLoad(code.getCurrentScope().getVariable(1));
        else if (! (owning instanceof ModuleExp))
            owning.loadHeapFrame(comp);
        else if (! comp.method.getStaticFlag())
            code.emitPushThis();
        else {
            if (comp.moduleInstanceVar == null || comp.moduleInstanceVar.dead()) {
                comp.moduleInstanceVar
                    = code.locals.current_scope.addVariable(code,
                                                            Type.javalangClassType,
                                                            "$class");
                comp.loadClassRef(comp.moduleClass);
                code.emitStore(comp.moduleInstanceVar);
            }
            code.emitLoad(comp.moduleInstanceVar);
        }
        /* #ifdef use:java.lang.invoke */
        code.emitPushMethodHandle(proc.checkMethod);
        /* #else */
        // comp.loadClassRef(proc.checkMethod.getDeclaringClass());
        // code.emitPushString(proc.checkMethod.getName());
        // code.emitInvokeStatic(lookupApplyHandleMethod);
        /* #endif */
        comp.compileConstant(proc.getProperty(PropertySet.nameKey, pname),
                             Target.pushObject);
        // If there are keyword arguments, we treat that as "unlimited" maxArgs,
        // so that ModuleBody.matchX methods call matchN.  A kludge, I guess.
        code.emitPushInt(proc.min_args
                         | ((proc.keywords == null ? proc.max_args : -1) << 12));
        code.emitInvoke(initModuleMethod);

        if (proc.properties != null) {
            int len = proc.properties.length;
            for (int i = 0;  i < len;  i += 2) {
                Object key = proc.properties[i];
                // Skip "name" property since we've taken care of that specially.
                if (key != null && key != PropertySet.nameKey) {
                    Object val = proc.properties[i+1];
                    code.emitDup(1);
                    Field pfld = null;
                    if (key == Procedure.validateApplyKey)
                        pfld = Compilation.typeProcedure
                            .getDeclaredField("validateApplyKey");
                    else if (key == Procedure.validateXApplyKey)
                        pfld = Compilation.typeProcedure
                            .getDeclaredField("validateXApplyKey");
                    else if (key == Procedure.compilerXKey)
                        pfld = Compilation.typeProcedure
                            .getDeclaredField("compilerXKey");
                    if (pfld != null)
                        code.emitGetStatic(pfld);
                    else
                        comp.compileConstant(key);
                    Target target = Target.pushObject;
                    if (val instanceof Expression)
                        ((Expression) val).compile(comp, target);
                    else
                        comp.compileConstant(val, target);
                    Method m = (ClassType.make("gnu.mapping.PropertySet")
                                .getDeclaredMethod("setProperty", 2));
                    code.emitInvokeVirtual(m);
                }
            }
        }
    }

    public void emit(Compilation comp) {
        CodeAttr code = comp.getCode();
        if (! field.getStaticFlag())
            code.emitPushThis();

        emitLoadModuleMethod(proc, comp);

        if (field.getStaticFlag())
            code.emitPutStatic(field);
        else
            code.emitPutField(field);
    }

    public void reportError(String message, Compilation comp) {
        String saveFile = comp.getFileName();
        int saveLine = comp.getLineNumber();
        int saveColumn = comp.getColumnNumber();
        comp.setLocation(proc);
        String name = proc.getName();
        StringBuffer sbuf = new StringBuffer(message);
        if (name == null)
            sbuf.append("unnamed procedure");
        else {
            sbuf.append("procedure ");
            sbuf.append(name);
        }
        comp.error('e', sbuf.toString());
        comp.setLine(saveFile, saveLine, saveColumn);
    }
}
